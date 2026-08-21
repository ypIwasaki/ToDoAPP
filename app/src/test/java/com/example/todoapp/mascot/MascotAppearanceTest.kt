package com.example.todoapp.mascot

import org.junit.Assert.assertEquals
import org.junit.Test

class MascotAppearanceTest {
    @Test
    fun valuesAreClampedToSupportedRanges() {
        assertEquals(
            MascotAppearance.MIN_SIZE_PERCENT,
            MascotAppearance.normalized(1, 1).sizePercent,
        )
        assertEquals(
            MascotAppearance.MAX_OPACITY_PERCENT,
            MascotAppearance.normalized(999, 999).opacityPercent,
        )
        assertEquals(
            false,
            MascotAppearance.normalized(100, 100, movementEnabled = false).movementEnabled,
        )
    }

    @Test
    fun quietHoursSupportOvernightRanges() {
        val appearance = MascotAppearance.normalized(
            sizePercent = 100,
            opacityPercent = 100,
            quietStartHour = 22,
            quietEndHour = 7,
            autoResumeDelaySeconds = 999,
        )

        assertEquals(true, appearance.isQuietHour(23))
        assertEquals(true, appearance.isQuietHour(6))
        assertEquals(false, appearance.isQuietHour(12))
        assertEquals(
            MascotAppearance.MAX_AUTO_RESUME_DELAY_SECONDS,
            appearance.autoResumeDelaySeconds,
        )
    }

    @Test
    fun percentageValuesProduceDimensionsAndAlpha() {
        val appearance = MascotAppearance.normalized(
            sizePercent = 125,
            opacityPercent = 65,
        )

        assertEquals(230, appearance.scaledDimension(184))
        assertEquals(0.65f, appearance.alpha, 0.0001f)
        assertEquals(true, appearance.movementEnabled)
    }
}
