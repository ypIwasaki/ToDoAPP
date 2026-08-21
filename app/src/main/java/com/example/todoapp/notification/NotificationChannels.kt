package com.example.todoapp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.todoapp.R

const val REMINDER_CHANNEL_ID = "task_deadline_alerts_v2"

fun createReminderNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = context.getString(R.string.notification_channel_description)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        enableVibration(true)
        enableLights(true)
        setShowBadge(true)
    }
    manager.createNotificationChannel(channel)
}
