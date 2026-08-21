package com.example.todoapp.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoapp.TodoApplication
import kotlinx.coroutines.CancellationException

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, INVALID_TASK_ID)
        val deadline = inputData.getLong(KEY_DEADLINE, INVALID_DEADLINE)
        val offset = inputData.getInt(KEY_OFFSET, INVALID_OFFSET)
        if (taskId == INVALID_TASK_ID || deadline == INVALID_DEADLINE || offset == INVALID_OFFSET) {
            return Result.failure()
        }

        val app = applicationContext as TodoApplication
        return try {
            app.reminderNotifier.deliverIfCurrent(taskId, deadline, offset)
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
        const val INVALID_TASK_ID = -1L
        const val INVALID_DEADLINE = -1L
        const val INVALID_OFFSET = -1
        const val MAX_RETRY_COUNT = 3

        fun uniqueWorkName(taskId: Long): String = "task-reminder-$taskId"
    }
}
