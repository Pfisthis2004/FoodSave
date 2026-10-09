package com.example.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.FoodDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered periodically by AlarmManager to check expiring foods offline.
 * Requires 0 internet connection; reads directly from Room database on-device.
 */
class ExpiryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FoodDatabase.getDatabase(context)
                val foods = db.foodDao().getAllActiveFoods().firstOrNull() ?: emptyList()
                if (foods.isNotEmpty()) {
                    NotificationHelper.checkAndNotifyExpiringFoods(context, foods, timingDays = 3)
                }
            } catch (_: Exception) {
                // Failsafe catch for broadcast receiver
            }
        }
    }
}
