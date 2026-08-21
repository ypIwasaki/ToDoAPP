package com.example.todoapp.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoapp.TodoApplication
import com.example.todoapp.mascot.MascotOverlayService
import kotlinx.coroutines.CancellationException

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, INVALID_TASK_ID)
        val snooze = inputData.getBoolean(KEY_SNOOZE, false)
        val snoozeTriggerAt = inputData.getLong(KEY_SNOOZE_TRIGGER_AT, INVALID_DEADLINE)
        val deadline = inputData.getLong(KEY_DEADLINE, INVALID_DEADLINE)
        val offset = inputData.getInt(KEY_OFFSET, INVALID_OFFSET)
        if (
            taskId == INVALID_TASK_ID ||
            (!snooze && (deadline == INVALID_DEADLINE || offset == INVALID_OFFSET)) ||
            (snooze && snoozeTriggerAt == INVALID_DEADLINE)
        ) {
            return Result.failure()
        }

        val app = applicationContext as TodoApplication
        return try {
            val result = if (snooze) {
                app.reminderNotifier.deliverSnooze(taskId, snoozeTriggerAt)
            } else {
                app.reminderNotifier.deliverIfCurrent(taskId, deadline, offset)
            }
            if (result == ReminderDeliveryResult.DELIVERED) {
                MascotOverlayService.notifyTaskReminder(applicationContext, taskId)
            }
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_DEADLINE = "deadline"
        const val KEY_OFFSET = "offset"
        const val KEY_SNOOZE = "snooze"
        const val KEY_SNOOZE_TRIGGER_AT = "snooze_trigger_at"
        const val INVALID_TASK_ID = -1L
        const val INVALID_DEADLINE = -1L
        const val INVALID_OFFSET = -1
        const val MAX_RETRY_COUNT = 3

        fun uniqueWorkName(taskId: Long): String = "task-reminder-$taskId"
        fun snoozeUniqueWorkName(taskId: Long): String = "task-snooze-$taskId"
    }
}
