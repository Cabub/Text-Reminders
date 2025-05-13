package com.cabub.textreminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment


@Composable
fun InputScreen(
    uiState: UiState,
    onMessageChange: (String)->Unit,
    onAddRecipient: ()->Unit,
    onUpdateRecipient: (Int,String)->Unit,
    onRemoveRecipient: (Int)->Unit,
    onConfirm: ()->Unit
) {
    Scaffold { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {

            OutlinedTextField(
                value = uiState.message,
                onValueChange = onMessageChange,
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text("Recipients:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                itemsIndexed(uiState.recipients) { idx, recipient ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { onUpdateRecipient(idx, it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { onRemoveRecipient(idx) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddRecipient) {
                Text("Add Recipient")
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onConfirm,
                enabled = uiState.message.isNotBlank() && uiState.recipients.any { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
fun ConfirmScreen(
    uiState: UiState,
    onBack: ()->Unit,
    onSend: ()->Unit
) {
    Scaffold { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Review Message", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(uiState.message, modifier = Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            Text("To:", style = MaterialTheme.typography.titleMedium)
            uiState.recipients.forEach {
                Text(it)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onSend) { Text("Send") }
            }
        }
    }
}

@Composable
fun StatusScreen(
    recipients: List<String>,
    statuses: List<SendStatus>,
    onCancel: ()->Unit,
    onDone: ()->Unit
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

            // Pair up each phone number with its status
            recipients.zip(statuses).forEach { (number, status) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (status) {
                        SendStatus.Pending -> CircularProgressIndicator(Modifier.size(20.dp))
                        SendStatus.Success -> Icon(Icons.Default.Check, contentDescription = "Sent")
                        is SendStatus.Failure -> Icon(
                            Icons.Default.Close,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error
                        )

                        SendStatus.Delivered -> Icon(Icons.Default.CheckCircle, contentDescription = "Delivered")
                    }
                    Spacer(Modifier.width(8.dp))

                    if (status is SendStatus.Failure) {
                        Text(
                            text = "$number: ${status.reason}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onDone)        { Text("Done")   }
            }
        }
    }
}
