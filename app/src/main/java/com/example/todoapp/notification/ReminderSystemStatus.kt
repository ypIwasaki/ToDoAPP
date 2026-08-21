package com.example.todoapp.notification

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class ReminderSystemStatus(
    val notificationsAllowed: Boolean = false,
    val headsUpAlertsAllowed: Boolean = false,
    val exactAlarmsAllowed: Boolean = false,
    val fullScreenAlertsAllowed: Boolean = false,
    val backgroundRestricted: Boolean = false,
)

fun readReminderSystemStatus(context: Context): ReminderSystemStatus {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val channel = notificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
    val notificationsAllowed =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            channel?.importance != NotificationManager.IMPORTANCE_NONE

    return ReminderSystemStatus(
        notificationsAllowed = notificationsAllowed,
        headsUpAlertsAllowed = notificationsAllowed &&
            channel != null &&
            channel.importance >= NotificationManager.IMPORTANCE_HIGH &&
            channel.lockscreenVisibility != Notification.VISIBILITY_SECRET,
        exactAlarmsAllowed = alarmManager.canScheduleExactAlarms(),
        fullScreenAlertsAllowed = notificationManager.canUseFullScreenIntent(),
        backgroundRestricted = activityManager.isBackgroundRestricted,
    )
}
