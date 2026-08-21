package com.example.todoapp.mascot

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object MascotAnnouncementScheduler {
    fun update(context: Context, frequency: MascotAnnouncementFrequency) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (frequency == MascotAnnouncementFrequency.OFF) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<MascotAnnouncementWorker>(
            frequency.intervalHours,
            TimeUnit.HOURS,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private const val UNIQUE_WORK_NAME = "mascot-periodic-announcement"
}
