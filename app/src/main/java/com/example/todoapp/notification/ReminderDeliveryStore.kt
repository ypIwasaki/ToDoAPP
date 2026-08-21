package com.example.todoapp.notification

import android.content.Context
import androidx.core.content.edit

class ReminderDeliveryStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun wasDelivered(taskId: Long, deadline: Long, offsetMinutes: Int): Boolean =
        preferences.getString(key(taskId), null) == reminderDeliveryToken(deadline, offsetMinutes)

    @Synchronized
    fun markDeliveredIfNew(taskId: Long, deadline: Long, offsetMinutes: Int): Boolean {
        val key = key(taskId)
        val token = reminderDeliveryToken(deadline, offsetMinutes)
        if (preferences.getString(key, null) == token) return false
        preferences.edit(commit = true) { putString(key, token) }
        return true
    }

    @Synchronized
    fun clear(taskId: Long) {
        preferences.edit { remove(key(taskId)) }
    }

    @Synchronized
    fun clearIfMatches(taskId: Long, deadline: Long, offsetMinutes: Int) {
        if (wasDelivered(taskId, deadline, offsetMinutes)) clear(taskId)
    }

    private fun key(taskId: Long): String = "task_$taskId"

    private companion object {
        const val PREFERENCES_NAME = "reminder_delivery_state"
    }
}

fun reminderDeliveryToken(deadline: Long, offsetMinutes: Int): String =
    "$deadline:$offsetMinutes"
