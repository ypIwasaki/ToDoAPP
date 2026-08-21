package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY isCompleted ASC,
                 CASE WHEN deadlineEpochMillis IS NULL THEN 1 ELSE 0 END,
                 deadlineEpochMillis ASC,
                 createdAt DESC
        """,
    )
    fun observeTasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Long): TaskWithSubtasks?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND deadlineEpochMillis IS NOT NULL")
    suspend fun getTasksWithPendingReminders(): List<TaskEntity>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Insert
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean, updatedAt: Long)

    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :subtaskId")
    suspend fun setSubtaskCompleted(subtaskId: Long, completed: Boolean)

    @Transaction
    suspend fun saveTask(task: TaskEntity, subtasks: List<SubtaskEntity>): Long {
        val taskId = if (task.id == 0L) {
            insertTask(task)
        } else {
            updateTask(task)
            task.id
        }

        deleteSubtasksForTask(taskId)
        if (subtasks.isNotEmpty()) {
            insertSubtasks(subtasks.map { it.copy(taskId = taskId) })
        }
        return taskId
    }
}
