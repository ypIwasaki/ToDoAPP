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
