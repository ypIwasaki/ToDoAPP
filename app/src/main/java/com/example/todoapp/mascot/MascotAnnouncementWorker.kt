package com.example.todoapp.mascot

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoapp.TodoApplication
import kotlinx.coroutines.flow.first
import java.time.LocalTime

internal class MascotAnnouncementWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val appearance = MascotAppearancePreferences(applicationContext).read()
        if (
            appearance.announcementFrequency == MascotAnnouncementFrequency.OFF ||
            appearance.isQuietHour(LocalTime.now().hour)
        ) return Result.success()

        val app = applicationContext as TodoApplication
        val tasks = app.taskRepository.observeTasks().first().map { it.task }
        val event = MascotTaskEventPlanner.overview(tasks, System.currentTimeMillis())
        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        if (MascotOverlayService.isRunning.value && powerManager.isInteractive) {
            MascotOverlayService.requestPeriodicAnnouncement(applicationContext)
        } else {
            MascotAnnouncementNotifier(applicationContext).notify(event)
        }
        return Result.success()
    }
}
