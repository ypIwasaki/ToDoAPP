package com.example.todoapp.notification

private const val MILLIS_PER_MINUTE = 60_000L

fun reminderTriggerAt(deadlineEpochMillis: Long, offsetMinutes: Int): Long =
    deadlineEpochMillis - (offsetMinutes * MILLIS_PER_MINUTE)
