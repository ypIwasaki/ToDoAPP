package com.example.todoapp.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.TaskWithSubtasks
import com.example.todoapp.notification.ReminderSystemStatus

@Composable
fun TaskListScreen(
    tasks: List<TaskWithSubtasks>,
    filter: TaskFilter,
    reminderSystemStatus: ReminderSystemStatus,
    onFilterChange: (TaskFilter) -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskChecked: (TaskWithSubtasks, Boolean) -> Unit,
    onSubtaskChecked: (Long, Boolean) -> Unit,
    onDeleteTask: (TaskWithSubtasks) -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAlertAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredTasks = remember(tasks, filter) {
        when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.ACTIVE -> tasks.filterNot { it.task.isCompleted }
            TaskFilter.COMPLETED -> tasks.filter { it.task.isCompleted }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { onFilterChange(item) },
                    label = { Text(item.label) },
                )
            }
        }

        ReminderStatusCard(
            status = reminderSystemStatus,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestFullScreenAlertAccess = onRequestFullScreenAlertAccess,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenAppSettings = onOpenAppSettings,
        )

        if (filteredTasks.isEmpty()) {
            EmptyTaskState(filter, Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredTasks, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        onEdit = { onEditTask(item.task.id) },
                        onTaskChecked = { onTaskChecked(item, it) },
                        onSubtaskChecked = onSubtaskChecked,
                        onDelete = { onDeleteTask(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(filter: TaskFilter, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (filter == TaskFilter.COMPLETED) "完了したタスクはありません" else "タスクはありません",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (filter == TaskFilter.COMPLETED) "タスクを完了するとここに表示されます" else "＋ボタンから最初のタスクを追加しましょう",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun TaskCard(
    item: TaskWithSubtasks,
    onEdit: () -> Unit,
    onTaskChecked: (Boolean) -> Unit,
    onSubtaskChecked: (Long, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(item.task.id) { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val task = item.task
    val subtasks = item.orderedSubtasks
    val completedSubtasks = subtasks.count { it.isCompleted }
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onTaskChecked,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = textDecoration,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else MaterialTheme.colorScheme.onSurface,
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "削除")
                }
            }

            Row(
                modifier = Modifier.padding(start = 56.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                task.deadlineEpochMillis?.let { deadline ->
                    val isOverdue = !task.isCompleted && deadline < System.currentTimeMillis()
                    AssistChip(
                        onClick = onEdit,
                        label = { Text(if (isOverdue) "期限切れ・${formatDeadline(deadline)}" else formatDeadline(deadline)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = if (isOverdue) {
                            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.error,
                                leadingIconContentColor = MaterialTheme.colorScheme.error,
                            )
                        } else androidx.compose.material3.AssistChipDefaults.assistChipColors(),
                    )
                }

                if (subtasks.isNotEmpty()) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text("$completedSubtasks/${subtasks.size}")
                        Icon(
                            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "サブタスクを閉じる" else "サブタスクを開く",
                        )
                    }
                }
            }

            if (subtasks.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { completedSubtasks.toFloat() / subtasks.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                subtasks.forEach { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onSubtaskChecked(subtask.id, it) },
                        )
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("タスクを削除しますか？") },
            text = { Text("「${task.title}」とサブタスクを削除します。この操作は元に戻せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }
}
