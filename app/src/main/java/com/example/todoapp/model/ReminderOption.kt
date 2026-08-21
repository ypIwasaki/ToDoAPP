package com.example.todoapp.model

enum class ReminderOption(
    val offsetMinutes: Int,
    val displayName: String,
) {
    AT_DEADLINE(0, "期限時"),
    TEN_MINUTES_BEFORE(10, "10分前"),
    ONE_HOUR_BEFORE(60, "1時間前"),
    ONE_DAY_BEFORE(24 * 60, "1日前"),
    ;

    companion object {
        fun fromMinutes(minutes: Int?): ReminderOption =
            entries.firstOrNull { it.offsetMinutes == minutes } ?: ONE_HOUR_BEFORE
    }
}
