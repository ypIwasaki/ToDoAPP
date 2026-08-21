package com.example.todoapp.model

data class SubtaskDraft(
    val id: Long = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class TaskDraft(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val hasDeadline: Boolean = true,
    val deadlineEpochMillis: Long,
    val reminder: ReminderOption = ReminderOption.ONE_HOUR_BEFORE,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val subtasks: List<SubtaskDraft> = emptyList(),
)
