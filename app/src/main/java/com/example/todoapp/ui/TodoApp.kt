package com.example.todoapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoapp.data.TaskWithSubtasks
import com.example.todoapp.notification.ReminderSystemStatus

@Composable
fun TodoApp(
    viewModel: TodoViewModel,
    reminderSystemStatus: ReminderSystemStatus,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAlertAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = viewModel.destination == AppDestination.EDITOR) {
        viewModel.closeEditor()
    }

    when (viewModel.destination) {
        AppDestination.MAIN -> MainHome(
            tasks = tasks,
            section = viewModel.mainSection,
            filter = viewModel.taskFilter,
            reminderSystemStatus = reminderSystemStatus,
            snackbarHostState = snackbarHostState,
            onSectionChange = viewModel::selectMainSection,
            onFilterChange = viewModel::changeTaskFilter,
            onCreateTask = viewModel::startCreate,
            onEditTask = viewModel::startEdit,
            onTaskChecked = viewModel::setTaskCompleted,
            onSubtaskChecked = viewModel::setSubtaskCompleted,
            onDeleteTask = viewModel::deleteTask,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestFullScreenAlertAccess = onRequestFullScreenAlertAccess,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenAppSettings = onOpenAppSettings,
        )

        AppDestination.EDITOR -> TaskEditorScreen(
            state = viewModel.editorState,
            onBack = viewModel::closeEditor,
            onSave = viewModel::saveEditor,
            onTitleChange = viewModel::updateTitle,
            onDescriptionChange = viewModel::updateDescription,
            onHasDeadlineChange = viewModel::updateHasDeadline,
            onDeadlineDateChange = viewModel::updateDeadlineDate,
            onDeadlineTimeChange = viewModel::updateDeadlineTime,
            onReminderChange = viewModel::updateReminder,
            onAddSubtask = viewModel::addSubtask,
            onSubtaskTitleChange = viewModel::updateSubtaskTitle,
            onSubtaskChecked = viewModel::updateSubtaskCompleted,
            onRemoveSubtask = viewModel::removeSubtask,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainHome(
    tasks: List<TaskWithSubtasks>,
    section: MainSection,
    filter: TaskFilter,
    reminderSystemStatus: ReminderSystemStatus,
    snackbarHostState: SnackbarHostState,
    onSectionChange: (MainSection) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onTaskChecked: (TaskWithSubtasks, Boolean) -> Unit,
    onSubtaskChecked: (Long, Boolean) -> Unit,
    onDeleteTask: (TaskWithSubtasks) -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAlertAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (section == MainSection.TASKS) "やること" else "カレンダー",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTask) {
                Icon(Icons.Outlined.Add, contentDescription = "タスクを追加")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = section == MainSection.TASKS,
                    onClick = { onSectionChange(MainSection.TASKS) },
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                    label = { Text("タスク") },
                )
                NavigationBarItem(
                    selected = section == MainSection.CALENDAR,
                    onClick = { onSectionChange(MainSection.CALENDAR) },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = { Text("カレンダー") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        when (section) {
            MainSection.TASKS -> TaskListScreen(
                tasks = tasks,
                filter = filter,
                reminderSystemStatus = reminderSystemStatus,
                onFilterChange = onFilterChange,
                onEditTask = onEditTask,
                onTaskChecked = onTaskChecked,
                onSubtaskChecked = onSubtaskChecked,
                onDeleteTask = onDeleteTask,
                onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestFullScreenAlertAccess = onRequestFullScreenAlertAccess,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenAppSettings = onOpenAppSettings,
                modifier = Modifier.padding(contentPadding),
            )

            MainSection.CALENDAR -> CalendarScreen(
                tasks = tasks,
                onEditTask = onEditTask,
                onTaskChecked = onTaskChecked,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}
