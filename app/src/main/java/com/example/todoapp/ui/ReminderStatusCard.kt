package com.example.todoapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.notification.ReminderSystemStatus

@Composable
fun ReminderStatusCard(
    status: ReminderSystemStatus,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAlertAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val issue = when {
        !status.notificationsAllowed -> ReminderIssue(
            title = "通知が無効です",
            description = "期限通知を受け取るには、ToDoの通知を許可してください。",
            actionLabel = "通知設定",
            icon = ReminderIssueIcon.NOTIFICATION,
            action = onOpenNotificationSettings,
        )
        !status.headsUpAlertsAllowed -> ReminderIssue(
            title = "ポップアップ通知が無効です",
            description = "画面上に通知を表示するため、期限のお知らせをポップアップ表示できる設定にしてください。",
            actionLabel = "通知設定",
            icon = ReminderIssueIcon.NOTIFICATION,
            action = onOpenNotificationSettings,
        )
        !status.exactAlarmsAllowed -> ReminderIssue(
            title = "アラームの許可が必要です",
            description = "アプリを閉じている間も期限どおりに通知するため、「アラームとリマインダー」を許可してください。",
            actionLabel = "設定を開く",
            icon = ReminderIssueIcon.ALARM,
            action = onRequestExactAlarmAccess,
        )
        !status.fullScreenAlertsAllowed -> ReminderIssue(
            title = "全画面通知の許可が必要です",
            description = "スリープ中に画面を点灯して期限を表示するため、ToDoの全画面通知を許可してください。",
            actionLabel = "設定を開く",
            icon = ReminderIssueIcon.FULL_SCREEN,
            action = onRequestFullScreenAlertAccess,
        )
        status.backgroundRestricted -> ReminderIssue(
            title = "バックグラウンド実行が制限されています",
            description = "Androidのバッテリー設定を「最適化」または「制限なし」に変更してください。「制限付き」では通知されません。",
            actionLabel = "アプリ設定",
            icon = ReminderIssueIcon.BATTERY,
            action = onOpenAppSettings,
        )
        else -> return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (issue.icon) {
                    ReminderIssueIcon.NOTIFICATION -> Icons.Outlined.NotificationsOff
                    ReminderIssueIcon.ALARM -> Icons.Outlined.AlarmOff
                    ReminderIssueIcon.FULL_SCREEN -> Icons.Outlined.Fullscreen
                    ReminderIssueIcon.BATTERY -> Icons.Outlined.BatteryAlert
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    issue.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Button(onClick = issue.action) {
                Text(issue.actionLabel)
            }
        }
    }
}

private data class ReminderIssue(
    val title: String,
    val description: String,
    val actionLabel: String,
    val icon: ReminderIssueIcon,
    val action: () -> Unit,
)

private enum class ReminderIssueIcon { NOTIFICATION, ALARM, FULL_SCREEN, BATTERY }
