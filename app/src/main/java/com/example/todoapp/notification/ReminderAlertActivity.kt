package com.example.todoapp.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.todoapp.MainActivity
import com.example.todoapp.ui.theme.TodoTheme

class ReminderAlertActivity : ComponentActivity() {
    private var alert by mutableStateOf(ReminderAlert())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        alert = ReminderAlert.from(intent)

        setContent {
            TodoTheme {
                ReminderAlertScreen(
                    alert = alert,
                    onDismiss = ::dismissAlert,
                    onOpenApp = ::openApp,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alert = ReminderAlert.from(intent)
    }

    private fun dismissAlert() {
        NotificationManagerCompat.from(this).cancel(alert.taskId.hashCode())
        finishAndRemoveTask()
    }

    private fun openApp() {
        NotificationManagerCompat.from(this).cancel(alert.taskId.hashCode())
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
        finishAndRemoveTask()
    }

    companion object {
        private const val EXTRA_TASK_ID = "alert_task_id"
        private const val EXTRA_TITLE = "alert_title"
        private const val EXTRA_DEADLINE = "alert_deadline"
        private const val EXTRA_OFFSET = "alert_offset"

        fun createIntent(
            context: Context,
            taskId: Long,
            title: String,
            deadline: Long,
            offset: Int,
        ): Intent = Intent(context, ReminderAlertActivity::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DEADLINE, deadline)
            putExtra(EXTRA_OFFSET, offset)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private data class ReminderAlert(
        val taskId: Long = -1L,
        val title: String = "タスク",
        val deadline: Long = 0L,
        val offset: Int = 0,
    ) {
        companion object {
            fun from(intent: Intent): ReminderAlert = ReminderAlert(
                taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L),
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "タスク" },
                deadline = intent.getLongExtra(EXTRA_DEADLINE, 0L),
                offset = intent.getIntExtra(EXTRA_OFFSET, 0),
            )
        }
    }

    @Composable
    private fun ReminderAlertScreen(
        alert: ReminderAlert,
        onDismiss: () -> Unit,
        onOpenApp: () -> Unit,
    ) {
        BackHandler(onBack = onDismiss)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = if (alert.offset == 0) "タスクの期限です" else "期限が近づいています",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (alert.deadline > 0L) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "期限：${formatReminderDeadline(alert.deadline)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("閉じる")
                    }
                    Button(
                        onClick = onOpenApp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("アプリを開く")
                    }
                }
            }
        }
    }
}
