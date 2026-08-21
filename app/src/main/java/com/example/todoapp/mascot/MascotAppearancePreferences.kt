package com.example.todoapp.mascot

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToInt

internal data class MascotAppearance(
    val sizePercent: Int,
    val opacityPercent: Int,
    val movementEnabled: Boolean,
) {
    val alpha: Float
        get() = opacityPercent / 100f

    fun scaledDimension(baseDp: Int): Int =
        (baseDp * sizePercent / 100f).roundToInt()

    companion object {
        const val DEFAULT_SIZE_PERCENT = 100
        const val MIN_SIZE_PERCENT = 50
        const val MAX_SIZE_PERCENT = 160
        const val DEFAULT_OPACITY_PERCENT = 100
        const val MIN_OPACITY_PERCENT = 20
        const val MAX_OPACITY_PERCENT = 100
        const val DEFAULT_MOVEMENT_ENABLED = true

        fun normalized(
            sizePercent: Int,
            opacityPercent: Int,
            movementEnabled: Boolean = DEFAULT_MOVEMENT_ENABLED,
        ): MascotAppearance = MascotAppearance(
            sizePercent = sizePercent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT),
            opacityPercent = opacityPercent.coerceIn(
                MIN_OPACITY_PERCENT,
                MAX_OPACITY_PERCENT,
            ),
            movementEnabled = movementEnabled,
        )
    }
}

internal class MascotAppearancePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): MascotAppearance = MascotAppearance.normalized(
        sizePercent = preferences.getInt(
            KEY_SIZE_PERCENT,
            MascotAppearance.DEFAULT_SIZE_PERCENT,
        ),
        opacityPercent = preferences.getInt(
            KEY_OPACITY_PERCENT,
            MascotAppearance.DEFAULT_OPACITY_PERCENT,
        ),
        movementEnabled = preferences.getBoolean(
            KEY_MOVEMENT_ENABLED,
            MascotAppearance.DEFAULT_MOVEMENT_ENABLED,
        ),
    )

    fun setSizePercent(value: Int) {
        val current = read()
        val normalized = MascotAppearance.normalized(
            sizePercent = value,
            opacityPercent = current.opacityPercent,
            movementEnabled = current.movementEnabled,
        )
        preferences.edit().putInt(KEY_SIZE_PERCENT, normalized.sizePercent).apply()
    }

    fun setOpacityPercent(value: Int) {
        val current = read()
        val normalized = MascotAppearance.normalized(
            sizePercent = current.sizePercent,
            opacityPercent = value,
            movementEnabled = current.movementEnabled,
        )
        preferences.edit().putInt(KEY_OPACITY_PERCENT, normalized.opacityPercent).apply()
    }

    fun setMovementEnabled(value: Boolean) {
        preferences.edit().putBoolean(KEY_MOVEMENT_ENABLED, value).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        const val PREFERENCES_NAME = "mascot_appearance"
        const val KEY_SIZE_PERCENT = "size_percent"
        const val KEY_OPACITY_PERCENT = "opacity_percent"
        const val KEY_MOVEMENT_ENABLED = "movement_enabled"
    }
}
