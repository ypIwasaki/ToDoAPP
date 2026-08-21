package com.example.todoapp.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasks(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
    )
    val subtasks: List<SubtaskEntity>,
) {
    val orderedSubtasks: List<SubtaskEntity>
        get() = subtasks.sortedWith(compareBy(SubtaskEntity::position, SubtaskEntity::id))
}
