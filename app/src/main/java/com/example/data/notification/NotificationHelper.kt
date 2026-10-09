package com.example.data.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.foodsave.MainActivity
import com.example.R
import com.example.data.model.FoodItem
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_ID = "foodsave_expiry_alerts"
    private const val CHANNEL_NAME = "Nhắc nhở hạn sử dụng thực phẩm"
    private const val CHANNEL_DESC = "Thông báo cảnh báo thực phẩm sắp hoặc đã hết hạn (hoạt động ngoại tuyến 100%)"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, id: Int, title: String, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, builder.build())
        } catch (_: SecurityException) {
            // Handled gracefully if permission denied
        }
    }

    fun checkAndNotifyExpiringFoods(
        context: Context,
        foods: List<FoodItem>,
        timingDays: Int = 3
    ) {
        foods.forEach { food ->
            val days = food.getRemainingDays()
            when {
                days < 0 -> {
                    sendNotification(
                        context,
                        (food.id + 1000).toInt(),
                        "🔴 ${food.displayEmoji()} ${food.name} đã quá hạn",
                        "${food.name} đã hết hạn ${-days} ngày trước. Vui lòng kiểm tra và xử lý."
                    )
                }
                days == 0L -> {
                    sendNotification(
                        context,
                        (food.id + 2000).toInt(),
                        "⚠️ ${food.displayEmoji()} ${food.name} hết hạn hôm nay!",
                        "${food.name} hết hạn hôm nay. Hãy sử dụng ngay để tránh lãng phí."
                    )
                }
                days == 1L -> {
                    sendNotification(
                        context,
                        (food.id + 3000).toInt(),
                        "⚠️ ${food.displayEmoji()} ${food.name} sẽ hết hạn ngày mai",
                        "${food.name} (${food.formattedQuantity()}) sẽ hết hạn vào ngày mai. Hãy ưu tiên nấu sớm!"
                    )
                }
                days in 2..timingDays -> {
                    sendNotification(
                        context,
                        (food.id + 4000).toInt(),
                        "${food.displayEmoji()} ${food.name} sắp hết hạn",
                        "${food.name} sẽ hết hạn trong $days ngày tới. Hãy sử dụng sớm."
                    )
                }
            }
        }
    }

    /**
     * Schedules daily offline check using Android AlarmManager.
     * Fires locally on device even without internet or network connectivity.
     */
    fun scheduleOfflineDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExpiryAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8888,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set alarm for 8:00 AM daily
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {
            // AlarmManager fallback
        }
    }
}
