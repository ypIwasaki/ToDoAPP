package com.example.todoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val deadlineEpochMillis: Long? = null,
    val reminderOffsetMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
