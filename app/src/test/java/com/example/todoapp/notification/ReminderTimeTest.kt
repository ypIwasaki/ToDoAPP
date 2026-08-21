package com.example.todoapp.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {
    @Test
    fun `deadline reminder uses deadline itself`() {
        assertEquals(1_800_000L, reminderTriggerAt(1_800_000L, 0))
    }

    @Test
    fun `ten minute reminder subtracts ten minutes`() {
        assertEquals(1_200_000L, reminderTriggerAt(1_800_000L, 10))
    }

    @Test
    fun `one day reminder subtracts twenty four hours`() {
        val deadline = 2L * 24 * 60 * 60 * 1_000
        assertEquals(24L * 60 * 60 * 1_000, reminderTriggerAt(deadline, 1_440))
    }
}
