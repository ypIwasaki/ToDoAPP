package com.example.todoapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.todoapp.data.TaskEntity
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(context)

    fun schedule(task: TaskEntity) {
        cancel(task.id)

        val deadline = task.deadlineEpochMillis ?: return
        val offset = task.reminderOffsetMinutes ?: return
        if (task.isCompleted || deadline <= System.currentTimeMillis()) return

        val triggerAt = reminderTriggerAt(deadline, offset)
            .coerceAtLeast(System.currentTimeMillis() + MINIMUM_DELAY_MILLIS)

        schedulePersistentBackup(task.id, deadline, offset, triggerAt)
        schedulePlatformAlarm(
            triggerAt = triggerAt,
            pendingIntent = reminderPendingIntent(task.id, deadline, offset),
        )
    }

    fun scheduleSnooze(task: TaskEntity, triggerAt: Long) {
        cancelSnooze(task.id)
        if (task.isCompleted) return
        val safeTriggerAt = triggerAt.coerceAtLeast(
            System.currentTimeMillis() + MINIMUM_DELAY_MILLIS,
        )
        scheduleSnoozeBackup(task.id, safeTriggerAt)
        schedulePlatformAlarm(
            triggerAt = safeTriggerAt,
            pendingIntent = snoozePendingIntent(task.id, safeTriggerAt),
        )
    }

    fun cancel(taskId: Long) {
        workManager.cancelUniqueWork(ReminderWorker.uniqueWorkName(taskId))
        cancelPlatformAlarm(requestCode(taskId, snooze = false))
        cancelSnooze(taskId)
    }

    private fun cancelSnooze(taskId: Long) {
        workManager.cancelUniqueWork(ReminderWorker.snoozeUniqueWorkName(taskId))
        cancelPlatformAlarm(requestCode(taskId, snooze = true))
    }

    private fun cancelPlatformAlarm(requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun schedulePlatformAlarm(
        triggerAt: Long,
        pendingIntent: PendingIntent,
    ) {
        if (alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Permission can be revoked between the capability check and this call.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    private fun schedulePersistentBackup(
        taskId: Long,
        deadline: Long,
        offset: Int,
        triggerAt: Long,
    ) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(
                (triggerAt - System.currentTimeMillis()).coerceAtLeast(MINIMUM_DELAY_MILLIS),
                TimeUnit.MILLISECONDS,
            )
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_TASK_ID to taskId,
                    ReminderWorker.KEY_DEADLINE to deadline,
                    ReminderWorker.KEY_OFFSET to offset,
                ),
            )
            .addTag(REMINDER_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            ReminderWorker.uniqueWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun scheduleSnoozeBackup(taskId: Long, triggerAt: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(
                (triggerAt - System.currentTimeMillis()).coerceAtLeast(MINIMUM_DELAY_MILLIS),
                TimeUnit.MILLISECONDS,
            )
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_TASK_ID to taskId,
                    ReminderWorker.KEY_SNOOZE to true,
                    ReminderWorker.KEY_SNOOZE_TRIGGER_AT to triggerAt,
                ),
            )
            .addTag(SNOOZE_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            ReminderWorker.snoozeUniqueWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun reminderPendingIntent(
        taskId: Long,
        deadline: Long,
        offset: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(taskId, snooze = false),
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_DEADLINE, deadline)
            putExtra(ReminderReceiver.EXTRA_OFFSET, offset)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun snoozePendingIntent(
        taskId: Long,
        triggerAt: Long,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(taskId, snooze = true),
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_SNOOZE, true)
            putExtra(ReminderReceiver.EXTRA_SNOOZE_TRIGGER_AT, triggerAt)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun requestCode(taskId: Long, snooze: Boolean): Int {
        val base = (taskId xor (taskId ushr 32)).toInt()
        return if (snooze) base xor SNOOZE_REQUEST_MASK else base
    }

    private companion object {
        const val MINIMUM_DELAY_MILLIS = 1_000L
        const val REMINDER_WORK_TAG = "task-reminders"
        const val SNOOZE_WORK_TAG = "task-snoozes"
        const val SNOOZE_REQUEST_MASK = Int.MIN_VALUE
    }
}
