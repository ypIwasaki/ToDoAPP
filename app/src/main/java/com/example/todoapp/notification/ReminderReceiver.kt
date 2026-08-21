package com.example.todoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoapp.TodoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val expectedDeadline = intent.getLongExtra(EXTRA_DEADLINE, -1L)
        val expectedOffset = intent.getIntExtra(EXTRA_OFFSET, -1)
        if (taskId < 0L || expectedDeadline < 0L || expectedOffset < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                app.reminderNotifier.deliverIfCurrent(
                    taskId = taskId,
                    expectedDeadline = expectedDeadline,
                    expectedOffset = expectedOffset,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DEADLINE = "deadline"
        const val EXTRA_OFFSET = "offset"
    }
}
