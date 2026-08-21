package com.example.todoapp.data

import com.example.todoapp.model.TaskDraft
import com.example.todoapp.notification.ReminderDeliveryStore
import com.example.todoapp.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val reminderDeliveryStore: ReminderDeliveryStore,
) {
    fun observeTasks(): Flow<List<TaskWithSubtasks>> = taskDao.observeTasks()

    suspend fun getTask(taskId: Long): TaskWithSubtasks? = taskDao.getTask(taskId)

    suspend fun saveTask(draft: TaskDraft): Long {
        val previousTask = draft.id.takeIf { it != 0L }?.let { taskDao.getTask(it)?.task }
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            id = draft.id,
            title = draft.title.trim(),
            description = draft.description.trim(),
            deadlineEpochMillis = draft.deadlineEpochMillis.takeIf { draft.hasDeadline },
            reminderOffsetMinutes = draft.reminder.offsetMinutes.takeIf { draft.hasDeadline },
            isCompleted = draft.isCompleted,
            createdAt = draft.createdAt,
            updatedAt = now,
        )
        val subtasks = draft.subtasks
            .filter { it.title.isNotBlank() }
            .mapIndexed { index, subtask ->
                SubtaskEntity(
                    id = subtask.id,
                    taskId = draft.id,
                    title = subtask.title.trim(),
                    isCompleted = subtask.isCompleted,
                    position = index,
                    createdAt = subtask.createdAt,
                )
            }

        val savedId = taskDao.saveTask(task, subtasks)
        val savedTask = task.copy(id = savedId)
        if (
            previousTask == null ||
            previousTask.deadlineEpochMillis != savedTask.deadlineEpochMillis ||
            previousTask.reminderOffsetMinutes != savedTask.reminderOffsetMinutes ||
            previousTask.isCompleted != savedTask.isCompleted
        ) {
            reminderDeliveryStore.clear(savedId)
        }
        scheduleUnlessAlreadyDelivered(savedTask)
        return savedId
    }

    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        taskDao.setTaskCompleted(taskId, completed, System.currentTimeMillis())
        val task = taskDao.getTask(taskId)?.task ?: return
        if (completed) {
            reminderScheduler.cancel(taskId)
        } else {
            reminderDeliveryStore.clear(taskId)
            reminderScheduler.schedule(task)
        }
    }

    suspend fun setSubtaskCompleted(subtaskId: Long, completed: Boolean) {
        taskDao.setSubtaskCompleted(subtaskId, completed)
    }

    suspend fun deleteTask(task: TaskEntity) {
        reminderScheduler.cancel(task.id)
        reminderDeliveryStore.clear(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun rescheduleAll() {
        taskDao.getTasksWithPendingReminders().forEach(::scheduleUnlessAlreadyDelivered)
    }

    private fun scheduleUnlessAlreadyDelivered(task: TaskEntity) {
        val deadline = task.deadlineEpochMillis
        val offset = task.reminderOffsetMinutes
        if (
            deadline != null &&
            offset != null &&
            reminderDeliveryStore.wasDelivered(task.id, deadline, offset)
        ) {
            reminderScheduler.cancel(task.id)
        } else {
            reminderScheduler.schedule(task)
        }
    }
}
