package com.example.todoapp.mascot

import android.content.Context
import androidx.core.content.edit

internal class MascotMessageHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun shouldShow(
        eventKey: String,
        now: Long = System.currentTimeMillis(),
        cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    ): Boolean {
        val previousKey = preferences.getString(KEY_EVENT, null)
        val previousTime = preferences.getLong(KEY_SHOWN_AT, 0L)
        if (previousKey == eventKey && now - previousTime < cooldownMillis) return false
        preferences.edit {
            putString(KEY_EVENT, eventKey)
            putLong(KEY_SHOWN_AT, now)
        }
        return true
    }

    private companion object {
        const val PREFERENCES_NAME = "mascot_message_history"
        const val KEY_EVENT = "last_event"
        const val KEY_SHOWN_AT = "last_shown_at"
        const val DEFAULT_COOLDOWN_MILLIS = 30 * 60 * 1_000L
    }
}
