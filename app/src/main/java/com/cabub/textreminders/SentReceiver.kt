package com.cabub.textreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SentReceiver", "Received intent: $intent")
        Log.d("SentReceiver", "Result code: $resultCode")

        val idx = intent.getIntExtra("index", -1)
        Log.d("SentReceiver", "Index: $idx")

        val status = if (resultCode == android.app.Activity.RESULT_OK)
            SendStatus.Success
        else
            SendStatus.Failure("Send error")

        Log.d("SentReceiver", "Status: $status")

        if (idx >= 0) {
            RemindersViewModel.getInstance().updateStatus(idx, status)
        }
    }
}
