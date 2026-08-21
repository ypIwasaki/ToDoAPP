package com.example.todoapp.ui

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日(E) H:mm", Locale.JAPAN)
private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPAN)
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年 M月", Locale.JAPAN)

fun formatDeadline(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(dateTimeFormatter)

fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

fun formatMonth(month: YearMonth): String = month.format(monthFormatter)

fun calendarMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmptyDays = firstDay.dayOfWeek.value - 1
    return List(42) { index ->
        val day = index - leadingEmptyDays + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
}
