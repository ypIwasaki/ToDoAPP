package com.example.todoapp.mascot

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import com.example.todoapp.MainActivity

internal object MascotAppLauncher {
    fun open(context: Context, taskId: Long? = null): Boolean {
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId?.hashCode() ?: REQUEST_CODE_OPEN_APP,
            MainActivity.createLaunchIntent(context, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val options = ActivityOptions.makeBasic().apply {
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
        }
        return try {
            pendingIntent.send(options.toBundle())
            true
        } catch (_: PendingIntent.CanceledException) {
            false
        }
    }

    private const val REQUEST_CODE_OPEN_APP = 20_001
}
