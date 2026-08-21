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
        schedulePlatformAlarm(task.id, deadline, offset, triggerAt)
    }

    fun cancel(taskId: Long) {
        workManager.cancelUniqueWork(ReminderWorker.uniqueWorkName(taskId))

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun schedulePlatformAlarm(
        taskId: Long,
        deadline: Long,
        offset: Int,
        triggerAt: Long,
    ) {
        val pendingIntent = reminderPendingIntent(taskId, deadline, offset)
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

    private fun reminderPendingIntent(
        taskId: Long,
        deadline: Long,
        offset: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(taskId),
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_DEADLINE, deadline)
            putExtra(ReminderReceiver.EXTRA_OFFSET, offset)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun requestCode(taskId: Long): Int = (taskId xor (taskId ushr 32)).toInt()

    private companion object {
        const val MINIMUM_DELAY_MILLIS = 1_000L
        const val REMINDER_WORK_TAG = "task-reminders"
    }
}
