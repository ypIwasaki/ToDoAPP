package com.example.todoapp.mascot

import android.content.Context
import androidx.core.content.edit

internal data class MascotRelativePosition(
    val xFraction: Float,
    val yFraction: Float,
) {
    fun normalized(): MascotRelativePosition = MascotRelativePosition(
        xFraction = xFraction.coerceIn(0f, 1f),
        yFraction = yFraction.coerceIn(0f, 1f),
    )
}

internal class MascotPositionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): MascotRelativePosition? {
        if (!preferences.contains(KEY_X_FRACTION) || !preferences.contains(KEY_Y_FRACTION)) {
            return null
        }
        return MascotRelativePosition(
            xFraction = preferences.getFloat(KEY_X_FRACTION, 1f),
            yFraction = preferences.getFloat(KEY_Y_FRACTION, 1f),
        ).normalized()
    }

    fun save(position: MascotRelativePosition) {
        val normalized = position.normalized()
        preferences.edit {
            putFloat(KEY_X_FRACTION, normalized.xFraction)
            putFloat(KEY_Y_FRACTION, normalized.yFraction)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "mascot_position"
        const val KEY_X_FRACTION = "x_fraction"
        const val KEY_Y_FRACTION = "y_fraction"
    }
}
