package com.cabub.textreminders

import android.telephony.PhoneNumberUtils.formatNumber
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType

private fun safeFormat(number: String) = formatNumber(number, "US") ?: number


@Composable
fun InputScreen(
    uiState: UiState,
    onMessageChange: (String) -> Unit,
    onAddRecipient: () -> Unit,
    onUpdateRecipient: (Int, String) -> Unit,
    onRemoveRecipient: (Int) -> Unit,
    onConfirm: () -> Unit,
    onFormatRecipient: (Int) -> Unit
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val messageIsValid = uiState.message.isNotBlank()
            Text("Message:", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                isError = !messageIsValid,
                supportingText = {
                    if (!messageIsValid) {
                        Text(
                            text = "Message cannot be empty",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            Text("Recipients:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                itemsIndexed(uiState.recipients) { idx, recipient ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = recipient.number,
                            onValueChange = { onUpdateRecipient(idx, it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(recipient.focusRequester)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && focusState.isCaptured) {
                                        onFormatRecipient(idx)
                                    }
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                showKeyboardOnFocus = true
                            ),
                            isError = !recipient.isValid,
                            supportingText = {
                                if (!recipient.isValid) {
                                    Text(
                                        text = recipient.validationMessage,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else ""
                            },
                        )
                        IconButton(
                            onClick = { onRemoveRecipient(idx) },
                            enabled = uiState.recipients.size > 1
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddRecipient) { Text("Add Recipient") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onConfirm,
                enabled = uiState.message.isNotBlank() && uiState.recipients.all {
                    it.isValid && it.number.isNotBlank() && !it.isDuplicate
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirm") }
        }
    }
}

@Composable
fun ConfirmScreen(
    uiState: UiState,
    onSend: () -> Unit
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Confirm Message:", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.message,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Confirm Recipients:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                itemsIndexed(uiState.recipients) { _, recipient ->
                    OutlinedTextField(
                        value = recipient.number,
                        readOnly = true,
                        onValueChange = {},
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSend,
                enabled = uiState.message.isNotBlank() && uiState.recipients.all { it.number.isNotBlank() && it.isValid && !it.isDuplicate },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send") }
        }
    }
}

@Composable
fun StatusScreen(
    uiState: UiState,
    onDone: () -> Unit
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            uiState.recipients.forEach { recipient ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (recipient.status) {
                        SendStatus.Pending -> CircularProgressIndicator(Modifier.size(30.dp))
                        SendStatus.Success -> Icon(Icons.Default.Check, contentDescription = "Sent")
                        is SendStatus.Failure -> Icon(
                            Icons.Default.Close,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error
                        )
                        SendStatus.Delivered -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Delivered"
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = recipient.number,
                        readOnly = true,
                        onValueChange = {},
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
