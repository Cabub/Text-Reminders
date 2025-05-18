package com.cabub.textreminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneNumberUtils.areSamePhoneNumber
import android.telephony.PhoneNumberUtils.formatNumber
import android.telephony.PhoneNumberUtils.isWellFormedSmsAddress
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.text.filter
import kotlin.text.get

sealed class SendStatus {
    data object Pending   : SendStatus()
    data object Success   : SendStatus()
    data object Delivered : SendStatus()
    data class Failure(val reason: String) : SendStatus()
}

data class Recipient(
    val number: String = "",
    val status: SendStatus = SendStatus.Pending,
    val isValid: Boolean = false,
    val isDuplicate: Boolean = false,
    val validationMessage: String = "",
    val focusRequester: FocusRequester = FocusRequester(),
)

data class UiState(
    val message: String = "",
    val recipients: List<Recipient> = listOf(Recipient())
)

class RemindersViewModel private constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateStatus(idx: Int, newStatus: SendStatus) {
        _uiState.update { state ->
            state.copy(
                recipients = state.recipients.mapIndexed { index, recipient ->
                    if (index == idx) {
                        recipient.copy(status = newStatus)
                    } else {
                        recipient
                    }
                }
            )
        }
    }

    fun updateMessage(new: String) =
        _uiState.update { it.copy(message = new) }

    fun addRecipient() {
        _uiState.update { it.copy(recipients = it.recipients + Recipient()) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _uiState.value.recipients.last().focusRequester.requestFocus()
        }
    }

    fun updateRecipient(idx: Int, value: String) =
        _uiState.update { state ->
            state.copy(
                recipients = state.recipients.mapIndexed { index, recipient ->
                    if (index == idx) {
                        recipient.copy(
                            number = value.filter { c -> c in "0123456789" },
                            isValid = value.isNotEmpty() && isWellFormedSmsAddress(value),
                            isDuplicate = state.recipients.count {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    areSamePhoneNumber(value, it.number, "US")
                                } else {
                                    value == it.number
                                }
                            } > 1
                        )
                    } else {
                        recipient
                    }
                }
            )
        }

    fun formatRecipient(idx: Int) {
            _uiState.update { state ->
                state.copy(recipients = state.recipients.mapIndexed { index, recipient ->
                    if (index == idx) {
                        recipient.copy(
                            number = formatNumber(recipient.number, "US") ?: recipient.number,
                        )
                    } else {
                        recipient
                    }
                })
        }
    }

    fun removeRecipient(idx: Int) =
        _uiState.update {
            it.copy(recipients = it.recipients.filterIndexed { i, _ -> i != idx })
        }

    fun resetAll() {
        _uiState.value = UiState()
    }

    fun sendAll(
        context: Context,
        recipients: List<Recipient>,
        message: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val subId = SubscriptionManager.getDefaultSmsSubscriptionId()
                context.getSystemService(SmsManager::class.java)
                    ?.createForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                _uiState.update {
                    it.copy(recipients = it.recipients.toMutableList().also { list ->
                        list.forEachIndexed { idx, recipient ->
                            list[idx] = recipient.copy(
                                status = SendStatus.Failure("No SMS manager available")
                            )
                        }
                    })
                }
                return@launch
            }

            recipients.forEachIndexed { idx, recipient ->
                try {
                    val sentPI = PendingIntent.getBroadcast(
                        context,
                        100 + idx, // Unique request code for each recipient
                        Intent(context, SentReceiver::class.java).apply {
                            action = SMS_SENT_ACTION
                            putExtra("index", idx)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val deliveredPI = PendingIntent.getBroadcast(
                        context,
                        200 + idx, // Unique request code for each recipient
                        Intent(context, DeliveredReceiver::class.java).apply {
                            action = SMS_DELIVERED_ACTION
                            putExtra("index", idx)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    Log.d("RemindersViewModel", "Creating sentPI for index $idx, requestCode: ${100 + idx}")
                    Log.d("RemindersViewModel", "Creating deliveredPI for index $idx, requestCode: ${200 + idx}")

                    smsManager.sendTextMessage(
                        recipient.number, null, message, sentPI, deliveredPI
                    )
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(recipients = it.recipients.toMutableList().also { list ->
                            list[idx] = recipient.copy(
                                status = SendStatus.Failure(e.message ?: "Send error")
                            )
                        })
                    }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RemindersViewModel? = null

        fun getInstance(): RemindersViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemindersViewModel().also { INSTANCE = it }
            }
        }
    }
}
