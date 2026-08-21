package com.example.todoapp.mascot

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal data class MascotPosition(val x: Int, val y: Int)

internal data class MascotMovementBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
) {
    fun clamp(position: MascotPosition): MascotPosition = MascotPosition(
        x = position.x.coerceIn(minX, maxX),
        y = position.y.coerceIn(minY, maxY),
    )

    fun toRelative(position: MascotPosition): MascotRelativePosition {
        val clamped = clamp(position)
        return MascotRelativePosition(
            xFraction = fraction(clamped.x, minX, maxX),
            yFraction = fraction(clamped.y, minY, maxY),
        )
    }

    fun fromRelative(position: MascotRelativePosition): MascotPosition {
        val normalized = position.normalized()
        return MascotPosition(
            x = interpolate(minX, maxX, normalized.xFraction),
            y = interpolate(minY, maxY, normalized.yFraction),
        )
    }

    private fun fraction(value: Int, minimum: Int, maximum: Int): Float =
        if (maximum <= minimum) 0f else (value - minimum).toFloat() / (maximum - minimum)

    private fun interpolate(minimum: Int, maximum: Int, fraction: Float): Int =
        (minimum + (maximum - minimum) * fraction).roundToInt()
}

internal data class MascotWindowSize(val width: Int, val height: Int)

internal object MascotMovementPlanner {
    private const val SPEED_DP_PER_SECOND = 52f
    private const val MIN_DURATION_MILLIS = 1_800L
    private const val MAX_DURATION_MILLIS = 12_000L

    fun fitWindowSize(
        requestedWidth: Int,
        requestedHeight: Int,
        availableWidth: Int,
        availableHeight: Int,
    ): MascotWindowSize {
        val safeRequestedWidth = requestedWidth.coerceAtLeast(1)
        val safeRequestedHeight = requestedHeight.coerceAtLeast(1)
        val safeAvailableWidth = availableWidth.coerceAtLeast(1)
        val safeAvailableHeight = availableHeight.coerceAtLeast(1)
        val scale = min(
            1f,
            min(
                safeAvailableWidth.toFloat() / safeRequestedWidth,
                safeAvailableHeight.toFloat() / safeRequestedHeight,
            ),
        )
        return MascotWindowSize(
            width = (safeRequestedWidth * scale).roundToInt().coerceAtLeast(1),
            height = (safeRequestedHeight * scale).roundToInt().coerceAtLeast(1),
        )
    }

    fun movementBounds(
        displayWidth: Int,
        displayHeight: Int,
        insetLeft: Int,
        insetTop: Int,
        insetRight: Int,
        insetBottom: Int,
        characterWidth: Int,
        characterHeight: Int,
    ): MascotMovementBounds {
        val minX = insetLeft.coerceAtLeast(0)
        val minY = insetTop.coerceAtLeast(0)
        val maxX = (displayWidth - insetRight - characterWidth).coerceAtLeast(minX)
        val maxY = (displayHeight - insetBottom - characterHeight).coerceAtLeast(minY)
        return MascotMovementBounds(minX, maxX, minY, maxY)
    }

    fun durationMillis(
        start: MascotPosition,
        end: MascotPosition,
        density: Float,
    ): Long {
        val distancePixels = hypot(
            (end.x - start.x).toDouble(),
            (end.y - start.y).toDouble(),
        )
        val speedPixelsPerSecond = SPEED_DP_PER_SECOND * density.coerceAtLeast(1f)
        return (distancePixels / speedPixelsPerSecond * 1_000.0)
            .roundToLong()
            .coerceIn(MIN_DURATION_MILLIS, MAX_DURATION_MILLIS)
    }
}
