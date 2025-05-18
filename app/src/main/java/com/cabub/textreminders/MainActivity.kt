package com.cabub.textreminders

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
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
    private val vm: RemindersViewModel by viewModels()

    private lateinit var sentReceiver: BroadcastReceiver
    private lateinit var deliveredReceiver: BroadcastReceiver

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sentReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val idx = intent.getIntExtra("index", -1)
                val status = if (resultCode == RESULT_OK)
                    SendStatus.Success
                else
                    SendStatus.Failure("Send error")
                vm.updateStatus(idx, status)
            }
        }
        deliveredReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val idx = intent.getIntExtra("index", -1)
                vm.updateStatus(idx, SendStatus.Delivered)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), RECEIVER_NOT_EXPORTED)
            registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION))
            registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION))
        }

        val smsPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Permission denied – SMS won’t send without it.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // ask once on startup
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
                                onConfirm           = { navController.navigate("confirm") }
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
                                recipients = uiState.recipients,
                                onDone     = {
                                    vm.resetAll()
                                    navController.popBackStack("input", false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(sentReceiver)
        unregisterReceiver(deliveredReceiver)
    }
}
