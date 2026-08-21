package com.example.todoapp.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todoapp.TodoApplication
import com.example.todoapp.data.TaskWithSubtasks
import com.example.todoapp.model.ReminderOption
import com.example.todoapp.model.SubtaskDraft
import com.example.todoapp.model.TaskDraft
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainSection { TASKS, CALENDAR, MASCOT }
enum class AppDestination { MAIN, EDITOR }
enum class TaskFilter(val label: String) { ALL("すべて"), ACTIVE("未完了"), COMPLETED("完了") }

data class EditorUiState(
    val draft: TaskDraft? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: Boolean = false,
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as TodoApplication).taskRepository

    val tasks = repository.observeTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    var destination by mutableStateOf(AppDestination.MAIN)
        private set
    var mainSection by mutableStateOf(MainSection.TASKS)
        private set
    var taskFilter by mutableStateOf(TaskFilter.ACTIVE)
        private set
    var editorState by mutableStateOf(EditorUiState())
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun selectMainSection(section: MainSection) {
        mainSection = section
    }

    fun changeTaskFilter(filter: TaskFilter) {
        taskFilter = filter
    }

    fun startCreate() {
        val tomorrowAtNine = LocalDate.now()
            .plusDays(1)
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        editorState = EditorUiState(
            draft = TaskDraft(deadlineEpochMillis = tomorrowAtNine),
        )
        destination = AppDestination.EDITOR
    }

    fun startEdit(taskId: Long) {
        destination = AppDestination.EDITOR
        editorState = EditorUiState(isLoading = true)
        viewModelScope.launch {
            val item = repository.getTask(taskId)
            if (item == null) {
                destination = AppDestination.MAIN
                _messages.emit("タスクが見つかりませんでした")
                return@launch
            }
            val fallbackDeadline = LocalDate.now()
                .plusDays(1)
                .atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            editorState = EditorUiState(
                draft = TaskDraft(
                    id = item.task.id,
                    title = item.task.title,
                    description = item.task.description,
                    hasDeadline = item.task.deadlineEpochMillis != null,
                    deadlineEpochMillis = item.task.deadlineEpochMillis ?: fallbackDeadline,
                    reminder = ReminderOption.fromMinutes(item.task.reminderOffsetMinutes),
                    isCompleted = item.task.isCompleted,
                    createdAt = item.task.createdAt,
                    subtasks = item.orderedSubtasks.map {
                        SubtaskDraft(
                            id = it.id,
                            title = it.title,
                            isCompleted = it.isCompleted,
                            createdAt = it.createdAt,
                        )
                    },
                ),
            )
        }
    }

    fun closeEditor() {
        if (!editorState.isSaving) {
            destination = AppDestination.MAIN
            editorState = EditorUiState()
        }
    }

    fun updateTitle(value: String) = updateDraft { copy(title = value) }
    fun updateDescription(value: String) = updateDraft { copy(description = value) }
    fun updateHasDeadline(value: Boolean) = updateDraft { copy(hasDeadline = value) }
    fun updateReminder(value: ReminderOption) = updateDraft { copy(reminder = value) }

    fun updateDeadlineDate(value: LocalDate) = updateDraft {
        val current = Instant.ofEpochMilli(deadlineEpochMillis).atZone(ZoneId.systemDefault())
        copy(
            deadlineEpochMillis = value
                .atTime(current.toLocalTime())
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
    }

    fun updateDeadlineTime(value: LocalTime) = updateDraft {
        val current = Instant.ofEpochMilli(deadlineEpochMillis).atZone(ZoneId.systemDefault())
        copy(
            deadlineEpochMillis = current.toLocalDate()
                .atTime(value)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
    }

    fun addSubtask() = updateDraft {
        copy(subtasks = subtasks + SubtaskDraft())
    }

    fun updateSubtaskTitle(index: Int, value: String) = updateDraft {
        copy(subtasks = subtasks.mapIndexed { itemIndex, item ->
            if (itemIndex == index) item.copy(title = value) else item
        })
    }

    fun updateSubtaskCompleted(index: Int, value: Boolean) = updateDraft {
        copy(subtasks = subtasks.mapIndexed { itemIndex, item ->
            if (itemIndex == index) item.copy(isCompleted = value) else item
        })
    }

    fun removeSubtask(index: Int) = updateDraft {
        copy(subtasks = subtasks.filterIndexed { itemIndex, _ -> itemIndex != index })
    }

    fun saveEditor() {
        val draft = editorState.draft ?: return
        if (draft.title.isBlank()) {
            editorState = editorState.copy(titleError = true)
            return
        }
        editorState = editorState.copy(isSaving = true, titleError = false)
        viewModelScope.launch {
            repository.saveTask(draft)
            destination = AppDestination.MAIN
            editorState = EditorUiState()
            _messages.emit(if (draft.id == 0L) "タスクを追加しました" else "タスクを更新しました")
        }
    }

    fun setTaskCompleted(item: TaskWithSubtasks, completed: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(item.task.id, completed)
        }
    }

    fun setSubtaskCompleted(subtaskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setSubtaskCompleted(subtaskId, completed)
        }
    }

    fun deleteTask(item: TaskWithSubtasks) {
        viewModelScope.launch {
            repository.deleteTask(item.task)
            _messages.emit("タスクを削除しました")
        }
    }

    private inline fun updateDraft(transform: TaskDraft.() -> TaskDraft) {
        val current = editorState.draft ?: return
        editorState = editorState.copy(
            draft = current.transform(),
            titleError = false,
        )
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodoViewModel(application) as T
    }
}
