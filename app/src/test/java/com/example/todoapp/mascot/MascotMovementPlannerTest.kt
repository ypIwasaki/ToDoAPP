package com.example.todoapp.mascot

import org.junit.Assert.assertEquals
import org.junit.Test

class MascotMovementPlannerTest {
    @Test
    fun movementBoundsKeepCharacterInsideSystemInsets() {
        val bounds = MascotMovementPlanner.movementBounds(
            displayWidth = 1_080,
            displayHeight = 2_400,
            insetLeft = 24,
            insetTop = 80,
            insetRight = 24,
            insetBottom = 120,
            characterWidth = 300,
            characterHeight = 600,
        )

        assertEquals(MascotMovementBounds(24, 756, 80, 1_680), bounds)
        assertEquals(
            MascotPosition(24, 1_680),
            bounds.clamp(MascotPosition(-100, 9_999)),
        )
    }

    @Test
    fun oversizedCharacterIsScaledToFitWithoutChangingAspectRatio() {
        assertEquals(
            MascotWindowSize(width = 240, height = 400),
            MascotMovementPlanner.fitWindowSize(
                requestedWidth = 600,
                requestedHeight = 1_000,
                availableWidth = 300,
                availableHeight = 400,
            ),
        )
    }

    @Test
    fun movementDurationHasLowerAndUpperLimits() {
        assertEquals(
            1_800L,
            MascotMovementPlanner.durationMillis(
                start = MascotPosition(0, 0),
                end = MascotPosition(0, 0),
                density = 3f,
            ),
        )
        assertEquals(
            12_000L,
            MascotMovementPlanner.durationMillis(
                start = MascotPosition(0, 0),
                end = MascotPosition(100_000, 100_000),
                density = 3f,
            ),
        )
    }
}
