package com.example.todoapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoapp.model.ReminderOption
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onHasDeadlineChange: (Boolean) -> Unit,
    onDeadlineDateChange: (LocalDate) -> Unit,
    onDeadlineTimeChange: (LocalTime) -> Unit,
    onReminderChange: (ReminderOption) -> Unit,
    onAddSubtask: () -> Unit,
    onSubtaskTitleChange: (Int, String) -> Unit,
    onSubtaskChecked: (Int, Boolean) -> Unit,
    onRemoveSubtask: (Int) -> Unit,
) {
    val isNewTask = state.draft?.id == 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewTask) "タスクを追加" else "タスクを編集") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = state.draft != null && !state.isSaving,
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { contentPadding ->
        if (state.isLoading || state.draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val draft = state.draft
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("基本情報", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("タスク名（必須）") },
                    placeholder = { Text("例：資料を提出する") },
                    singleLine = true,
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text("タスク名を入力してください") }
                    } else null,
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("メモ") },
                    placeholder = { Text("詳細や補足を入力") },
                    minLines = 3,
                    maxLines = 6,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DeadlineSection(
                    hasDeadline = draft.hasDeadline,
                    deadlineEpochMillis = draft.deadlineEpochMillis,
                    reminder = draft.reminder,
                    onHasDeadlineChange = onHasDeadlineChange,
                    onDateChange = onDeadlineDateChange,
                    onTimeChange = onDeadlineTimeChange,
                    onReminderChange = onReminderChange,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("サブタスク", style = MaterialTheme.typography.titleMedium)
                Text(
                    "大きなタスクを、完了を確認できる小さな手順に分けられます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                draft.subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onSubtaskChecked(index, it) },
                        )
                        OutlinedTextField(
                            value = subtask.title,
                            onValueChange = { onSubtaskTitleChange(index, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("サブタスク ${index + 1}") },
                            singleLine = true,
                        )
                        IconButton(onClick = { onRemoveSubtask(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "サブタスクを削除")
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddSubtask,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("サブタスクを追加", modifier = Modifier.padding(start = 8.dp))
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (isNewTask) "タスクを追加" else "変更を保存")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineSection(
    hasDeadline: Boolean,
    deadlineEpochMillis: Long,
    reminder: ReminderOption,
    onHasDeadlineChange: (Boolean) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onReminderChange: (ReminderOption) -> Unit,
) {
    val context = LocalContext.current
    val deadline = remember(deadlineEpochMillis) {
        Instant.ofEpochMilli(deadlineEpochMillis).atZone(ZoneId.systemDefault())
    }
    var reminderMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("期限", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hasDeadline) "日付と時刻を設定" else "期限なし",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = hasDeadline, onCheckedChange = onHasDeadlineChange)
    }

    if (hasDeadline) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DateTimeCard(
                icon = {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                },
                label = "日付",
                value = formatFullDate(deadline.toLocalDate()),
                onClick = {
                    showDatePicker(context, deadline.toLocalDate(), onDateChange)
                },
                modifier = Modifier.weight(1.35f),
            )
            DateTimeCard(
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                label = "時刻",
                value = "%02d:%02d".format(deadline.hour, deadline.minute),
                onClick = {
                    showTimePicker(context, deadline.toLocalTime(), onTimeChange)
                },
                modifier = Modifier.weight(0.8f),
            )
        }

        ExposedDropdownMenuBox(
            expanded = reminderMenuExpanded,
            onExpandedChange = { reminderMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = reminder.displayName,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                label = { Text("通知タイミング") },
                leadingIcon = {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderMenuExpanded)
                },
            )
            ExposedDropdownMenu(
                expanded = reminderMenuExpanded,
                onDismissRequest = { reminderMenuExpanded = false },
            ) {
                ReminderOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onReminderChange(option)
                            reminderMenuExpanded = false
                        },
                    )
                }
            }
        }

        if (deadlineEpochMillis < System.currentTimeMillis()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "期限が過去になっています。このタスクの通知は予約されません。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else {
            Text(
                "「アラームとリマインダー」が未許可の場合、通知時刻が前後することがあります。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DateTimeCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun showDatePicker(
    context: Context,
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onDateSelected(LocalDate.of(year, month + 1, day)) },
        currentDate.year,
        currentDate.monthValue - 1,
        currentDate.dayOfMonth,
    ).show()
}

private fun showTimePicker(
    context: Context,
    currentTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
        currentTime.hour,
        currentTime.minute,
        true,
    ).show()
}
