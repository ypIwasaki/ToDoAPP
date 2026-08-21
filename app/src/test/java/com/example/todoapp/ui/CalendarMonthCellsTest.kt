package com.example.todoapp.ui

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarMonthCellsTest {
    @Test
    fun `calendar always returns six monday-first weeks`() {
        val cells = calendarMonthCells(YearMonth.of(2026, 1))

        assertEquals(42, cells.size)
        assertNull(cells[0])
        assertEquals(LocalDate.of(2026, 1, 1), cells[3])
        assertEquals(LocalDate.of(2026, 1, 31), cells[33])
        assertNull(cells[34])
    }

    @Test
    fun `calendar includes leap day`() {
        val cells = calendarMonthCells(YearMonth.of(2028, 2))

        assertEquals(LocalDate.of(2028, 2, 29), cells.filterNotNull().last())
        assertEquals(29, cells.filterNotNull().size)
    }
}
