package com.cabub.textreminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val message: String = "",
    val recipients: List<String> = listOf("")
)

sealed class SendStatus {
    data object Pending   : SendStatus()
    data object Success   : SendStatus()
    data object Delivered : SendStatus()
    data class Failure(val reason: String) : SendStatus()
}

class RemindersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _statusList = MutableStateFlow<List<SendStatus>>(emptyList())
    val statusList: StateFlow<List<SendStatus>> = _statusList

    fun updateStatus(idx: Int, newStatus: SendStatus) {
        _statusList.update { list ->
            list.toMutableList().also { it[idx] = newStatus }
        }
    }

    fun updateMessage(new: String) =
        _uiState.update { it.copy(message = new) }

    fun addRecipient() =
        _uiState.update { it.copy(recipients = it.recipients + "") }

    fun updateRecipient(idx: Int, value: String) =
        _uiState.update {
            it.copy(recipients = it.recipients.toMutableList().also { list ->
                list[idx] = value
            })
        }

    fun removeRecipient(idx: Int) =
        _uiState.update {
            it.copy(recipients = it.recipients.filterIndexed { i, _ -> i != idx })
        }

    fun resetAll() {
        _statusList.value = emptyList()
        _uiState.value = UiState()
    }

    fun sendAll(
        context: Context,
        recipients: List<String>,
        message: String
    ) {
        _statusList.value = List(recipients.size) { SendStatus.Pending }

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
                // Fallback if no SmsManager found
                _statusList.value = List(recipients.size) {
                    SendStatus.Failure("SMS manager not available")
                }
                return@launch
            }

            recipients.forEachIndexed { idx, number ->
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
                        number, null, message, sentPI, deliveredPI
                    )

                } catch (e: Exception) {
                    _statusList.update { list ->
                        list.toMutableList().apply {
                            set(idx, SendStatus.Failure(e.message ?: "Send error"))
                        }
                    }
                }
            }
        }
    }
}
