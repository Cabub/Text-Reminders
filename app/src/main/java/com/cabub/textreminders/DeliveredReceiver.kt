package com.cabub.textreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DeliveredReceiver", "Received intent: $intent")

        val idx = intent.getIntExtra("index", -1)
        Log.d("DeliveredReceiver", "Index: $idx")

        if (idx >= 0) {
            RemindersViewModel.getInstance().updateStatus(idx, SendStatus.Delivered)
        }
    }
}
