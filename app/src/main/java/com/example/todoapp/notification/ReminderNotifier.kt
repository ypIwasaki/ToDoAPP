package com.example.todoapp.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todoapp.MainActivity
import com.example.todoapp.R
import com.example.todoapp.data.TaskDao
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReminderDeliveryResult {
    DELIVERED,
    ALREADY_DELIVERED,
    STALE,
    NOTIFICATIONS_DISABLED,
}

class ReminderNotifier(
    private val context: Context,
    private val taskDao: TaskDao,
    private val deliveryStore: ReminderDeliveryStore,
) {
    suspend fun deliverSnooze(
        taskId: Long,
        expectedTriggerAt: Long,
    ): ReminderDeliveryResult {
        val task = taskDao.getTask(taskId)?.task ?: return ReminderDeliveryResult.STALE
        if (task.isCompleted) return ReminderDeliveryResult.STALE

        val notifications = NotificationManagerCompat.from(context)
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED ||
            !notifications.areNotificationsEnabled()
        ) return ReminderDeliveryResult.NOTIFICATIONS_DISABLED

        val deliveryId = taskId xor Long.MIN_VALUE
        if (
            !deliveryStore.markDeliveredIfNew(
                deliveryId,
                expectedTriggerAt,
                SNOOZE_DELIVERY_OFFSET,
            )
        ) return ReminderDeliveryResult.ALREADY_DELIVERED

        val deadline = task.deadlineEpochMillis ?: System.currentTimeMillis()
        return try {
            createReminderNotificationChannel(context)
            notifications.notify(
                taskId.hashCode(),
                buildNotification(
                    taskId = taskId,
                    title = task.title,
                    deadline = deadline,
                    offset = SNOOZE_NOTIFICATION_OFFSET,
                    titleOverride = context.getString(R.string.notification_snooze_title),
                ),
            )
            ReminderDeliveryResult.DELIVERED
        } catch (_: SecurityException) {
            deliveryStore.clearIfMatches(
                deliveryId,
                expectedTriggerAt,
                SNOOZE_DELIVERY_OFFSET,
            )
            ReminderDeliveryResult.NOTIFICATIONS_DISABLED
        } catch (exception: Exception) {
            deliveryStore.clearIfMatches(
                deliveryId,
                expectedTriggerAt,
                SNOOZE_DELIVERY_OFFSET,
            )
            throw exception
        }
    }

    suspend fun deliverIfCurrent(
        taskId: Long,
        expectedDeadline: Long,
        expectedOffset: Int,
    ): ReminderDeliveryResult {
        val task = taskDao.getTask(taskId)?.task ?: return ReminderDeliveryResult.STALE
        if (
            task.isCompleted ||
            task.deadlineEpochMillis != expectedDeadline ||
            task.reminderOffsetMinutes != expectedOffset
        ) return ReminderDeliveryResult.STALE

        val notifications = NotificationManagerCompat.from(context)
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED ||
            !notifications.areNotificationsEnabled()
        ) return ReminderDeliveryResult.NOTIFICATIONS_DISABLED

        if (!deliveryStore.markDeliveredIfNew(taskId, expectedDeadline, expectedOffset)) {
            return ReminderDeliveryResult.ALREADY_DELIVERED
        }

        return try {
            createReminderNotificationChannel(context)
            notifications.notify(
                taskId.hashCode(),
                buildNotification(taskId, task.title, expectedDeadline, expectedOffset),
            )
            ReminderDeliveryResult.DELIVERED
        } catch (_: SecurityException) {
            deliveryStore.clearIfMatches(taskId, expectedDeadline, expectedOffset)
            ReminderDeliveryResult.NOTIFICATIONS_DISABLED
        } catch (exception: Exception) {
            deliveryStore.clearIfMatches(taskId, expectedDeadline, expectedOffset)
            throw exception
        }
    }

    private fun buildNotification(
        taskId: Long,
        title: String,
        deadline: Long,
        offset: Int,
        titleOverride: String? = null,
    ): android.app.Notification {
        val openAppIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alertIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            ReminderAlertActivity.createIntent(context, taskId, title, deadline, offset),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deadlineText = formatReminderDeadline(deadline)

        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                titleOverride ?: context.getString(
                    if (offset == 0) R.string.notification_due_title else R.string.notification_title,
                ),
            )
            .setContentText("$title ・ $deadlineText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n期限：$deadlineText"))
            .setContentIntent(openAppIntent)
            .setFullScreenIntent(alertIntent, true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }

    private companion object {
        const val SNOOZE_NOTIFICATION_OFFSET = 1
        const val SNOOZE_DELIVERY_OFFSET = Int.MAX_VALUE
    }
}

fun formatReminderDeadline(deadline: Long): String = Instant.ofEpochMilli(deadline)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("M月d日(E) H:mm", Locale.JAPAN))
