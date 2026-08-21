package com.example.todoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoapp.TodoApplication
import com.example.todoapp.mascot.MascotOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0L) return
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        val expectedDeadline = intent.getLongExtra(EXTRA_DEADLINE, -1L)
        val expectedOffset = intent.getIntExtra(EXTRA_OFFSET, -1)
        val snoozeTriggerAt = intent.getLongExtra(EXTRA_SNOOZE_TRIGGER_AT, -1L)
        if (
            (!snooze && (expectedDeadline < 0L || expectedOffset < 0)) ||
            (snooze && snoozeTriggerAt < 0L)
        ) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TodoApplication
                val result = if (snooze) {
                    app.reminderNotifier.deliverSnooze(taskId, snoozeTriggerAt)
                } else {
                    app.reminderNotifier.deliverIfCurrent(
                        taskId = taskId,
                        expectedDeadline = expectedDeadline,
                        expectedOffset = expectedOffset,
                    )
                }
                if (result == ReminderDeliveryResult.DELIVERED) {
                    MascotOverlayService.notifyTaskReminder(context, taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DEADLINE = "deadline"
        const val EXTRA_OFFSET = "offset"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_SNOOZE_TRIGGER_AT = "snooze_trigger_at"
    }
}
