package com.cabub.textreminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneNumberUtils.areSamePhoneNumber
import android.telephony.PhoneNumberUtils.isWellFormedSmsAddress
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.text.filter

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

class RemindersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateStatus(idx: Int, newStatus: SendStatus) {
        _uiState.update { state ->
            state.copy(
                recipients = state.recipients.toMutableList().also { list ->
                    list[idx] = list[idx].copy(status = newStatus)
                }
            )
        }
    }

    fun updateMessage(new: String) =
        _uiState.update { it.copy(message = new) }

    fun addRecipient() =
        _uiState.update { it.copy(recipients = it.recipients + Recipient()) }

    fun updateRecipient(idx: Int, value: String) =
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(recipients = it.recipients.toMutableList().also { list ->
                    list[idx] = Recipient(
                        value.filter { c -> c in "0123456789" },
                        list[idx].status,
                        isValid = value.isNotEmpty() && isWellFormedSmsAddress(value),
                        isDuplicate = list.count {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                areSamePhoneNumber(value, it.number, "US")
                            } else {
                                value == it.number
                            }
                        } > 1
                    )
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
                        context, idx,
                        Intent(SMS_SENT_ACTION).putExtra("index", idx),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val deliveredPI = PendingIntent.getBroadcast(
                        context, idx + 1000,
                        Intent(SMS_DELIVERED_ACTION).putExtra("index", idx),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

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
}
