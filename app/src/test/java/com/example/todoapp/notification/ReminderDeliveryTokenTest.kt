package com.example.todoapp.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReminderDeliveryTokenTest {
    @Test
    fun `same deadline and offset produce the same token`() {
        assertEquals(
            reminderDeliveryToken(deadline = 1_800_000L, offsetMinutes = 10),
            reminderDeliveryToken(deadline = 1_800_000L, offsetMinutes = 10),
        )
    }

    @Test
    fun `changing deadline or offset produces a new token`() {
        val original = reminderDeliveryToken(deadline = 1_800_000L, offsetMinutes = 10)

        assertNotEquals(
            original,
            reminderDeliveryToken(deadline = 2_000_000L, offsetMinutes = 10),
        )
        assertNotEquals(
            original,
            reminderDeliveryToken(deadline = 1_800_000L, offsetMinutes = 60),
        )
    }
}
