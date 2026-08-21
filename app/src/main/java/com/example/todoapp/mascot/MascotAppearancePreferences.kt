package com.example.todoapp.mascot

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToInt

internal data class MascotAppearance(
    val sizePercent: Int,
    val opacityPercent: Int,
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

        fun normalized(
            sizePercent: Int,
            opacityPercent: Int,
        ): MascotAppearance = MascotAppearance(
            sizePercent = sizePercent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT),
            opacityPercent = opacityPercent.coerceIn(
                MIN_OPACITY_PERCENT,
                MAX_OPACITY_PERCENT,
            ),
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
    )

    fun setSizePercent(value: Int) {
        val normalized = MascotAppearance.normalized(
            sizePercent = value,
            opacityPercent = read().opacityPercent,
        )
        preferences.edit().putInt(KEY_SIZE_PERCENT, normalized.sizePercent).apply()
    }

    fun setOpacityPercent(value: Int) {
        val normalized = MascotAppearance.normalized(
            sizePercent = read().sizePercent,
            opacityPercent = value,
        )
        preferences.edit().putInt(KEY_OPACITY_PERCENT, normalized.opacityPercent).apply()
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
    }
}
