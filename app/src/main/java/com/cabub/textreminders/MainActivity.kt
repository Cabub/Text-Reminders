package com.cabub.textreminders

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cabub.textreminders.ui.theme.TextRemindersTheme

class MainActivity : ComponentActivity() {
    private val vm = RemindersViewModel.getInstance()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val smsPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Permission denied – SMS won’t send without it.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                Toast.makeText(
                    this,
                    "We need SMS permission to send reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }

        setContent {
            TextRemindersTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {

                    val navController = rememberNavController()

                    NavHost(navController, startDestination = "input") {
                        composable("input") {
                            val uiState by vm.uiState.collectAsState()
                            InputScreen(
                                uiState             = uiState,
                                onMessageChange     = vm::updateMessage,
                                onAddRecipient      = vm::addRecipient,
                                onUpdateRecipient   = vm::updateRecipient,
                                onRemoveRecipient   = vm::removeRecipient,
                                onConfirm           = { navController.navigate("confirm") },
                                onFormatRecipient   = vm::formatRecipient
                            )
                        }
                        composable("confirm") {
                            val uiState by vm.uiState.collectAsState()
                            val ctx = LocalContext.current
                            ConfirmScreen(
                                uiState = uiState,
                                onSend  = {
                                    vm.sendAll(ctx, uiState.recipients, uiState.message)
                                    navController.navigate("status")
                                }
                            )
                        }
                        composable("status") {
                            val uiState    by vm.uiState.collectAsState()
                            StatusScreen(
                                uiState,
                                onDone     = {
                                    navController.popBackStack("input", false)
                                    vm.resetState()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
