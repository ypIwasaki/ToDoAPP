package com.example.todoapp.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.todoapp.TodoApplication
import kotlinx.coroutines.CancellationException

class ReminderRescheduleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as TodoApplication).taskRepository.rescheduleAll()
        Result.success()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "reschedule-task-reminders"
        private const val MAX_RETRY_COUNT = 3

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderRescheduleWorker>().build(),
            )
        }
    }
}
