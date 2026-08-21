package com.example.todoapp.mascot

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.math.roundToInt

enum class MascotAnnouncementFrequency(
    val storageValue: String,
    val intervalHours: Long,
) {
    OFF("off", 24),
    QUIET("quiet", 6),
    NORMAL("normal", 3),
    LIVELY("lively", 1),
    ;

    companion object {
        fun fromStorage(value: String?): MascotAnnouncementFrequency =
            entries.firstOrNull { it.storageValue == value } ?: NORMAL
    }
}

internal data class MascotAppearance(
    val sizePercent: Int,
    val opacityPercent: Int,
    val movementEnabled: Boolean,
    val interactionsEnabled: Boolean,
    val announcementFrequency: MascotAnnouncementFrequency,
    val quietStartHour: Int,
    val quietEndHour: Int,
    val autoResumeDelaySeconds: Int,
) {
    val alpha: Float
        get() = opacityPercent / 100f

    fun scaledDimension(baseDp: Int): Int =
        (baseDp * sizePercent / 100f).roundToInt()

    fun isQuietHour(hour: Int): Boolean {
        val normalizedHour = hour.coerceIn(0, 23)
        return when {
            quietStartHour == quietEndHour -> false
            quietStartHour < quietEndHour ->
                normalizedHour in quietStartHour until quietEndHour
            else ->
                normalizedHour >= quietStartHour || normalizedHour < quietEndHour
        }
    }

    companion object {
        const val DEFAULT_SIZE_PERCENT = 100
        const val MIN_SIZE_PERCENT = 50
        const val MAX_SIZE_PERCENT = 160
        const val DEFAULT_OPACITY_PERCENT = 100
        const val MIN_OPACITY_PERCENT = 20
        const val MAX_OPACITY_PERCENT = 100
        const val DEFAULT_MOVEMENT_ENABLED = true
        const val DEFAULT_INTERACTIONS_ENABLED = true
        val DEFAULT_ANNOUNCEMENT_FREQUENCY = MascotAnnouncementFrequency.NORMAL
        const val DEFAULT_QUIET_START_HOUR = 22
        const val DEFAULT_QUIET_END_HOUR = 7
        const val DEFAULT_AUTO_RESUME_DELAY_SECONDS = 15
        const val MIN_AUTO_RESUME_DELAY_SECONDS = 5
        const val MAX_AUTO_RESUME_DELAY_SECONDS = 60

        fun normalized(
            sizePercent: Int,
            opacityPercent: Int,
            movementEnabled: Boolean = DEFAULT_MOVEMENT_ENABLED,
            interactionsEnabled: Boolean = DEFAULT_INTERACTIONS_ENABLED,
            announcementFrequency: MascotAnnouncementFrequency =
                DEFAULT_ANNOUNCEMENT_FREQUENCY,
            quietStartHour: Int = DEFAULT_QUIET_START_HOUR,
            quietEndHour: Int = DEFAULT_QUIET_END_HOUR,
            autoResumeDelaySeconds: Int = DEFAULT_AUTO_RESUME_DELAY_SECONDS,
        ): MascotAppearance = MascotAppearance(
            sizePercent = sizePercent.coerceIn(MIN_SIZE_PERCENT, MAX_SIZE_PERCENT),
            opacityPercent = opacityPercent.coerceIn(
                MIN_OPACITY_PERCENT,
                MAX_OPACITY_PERCENT,
            ),
            movementEnabled = movementEnabled,
            interactionsEnabled = interactionsEnabled,
            announcementFrequency = announcementFrequency,
            quietStartHour = quietStartHour.coerceIn(0, 23),
            quietEndHour = quietEndHour.coerceIn(0, 23),
            autoResumeDelaySeconds = autoResumeDelaySeconds.coerceIn(
                MIN_AUTO_RESUME_DELAY_SECONDS,
                MAX_AUTO_RESUME_DELAY_SECONDS,
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
        movementEnabled = preferences.getBoolean(
            KEY_MOVEMENT_ENABLED,
            MascotAppearance.DEFAULT_MOVEMENT_ENABLED,
        ),
        interactionsEnabled = preferences.getBoolean(
            KEY_INTERACTIONS_ENABLED,
            MascotAppearance.DEFAULT_INTERACTIONS_ENABLED,
        ),
        announcementFrequency = MascotAnnouncementFrequency.fromStorage(
            preferences.getString(KEY_ANNOUNCEMENT_FREQUENCY, null),
        ),
        quietStartHour = preferences.getInt(
            KEY_QUIET_START_HOUR,
            MascotAppearance.DEFAULT_QUIET_START_HOUR,
        ),
        quietEndHour = preferences.getInt(
            KEY_QUIET_END_HOUR,
            MascotAppearance.DEFAULT_QUIET_END_HOUR,
        ),
        autoResumeDelaySeconds = preferences.getInt(
            KEY_AUTO_RESUME_DELAY_SECONDS,
            MascotAppearance.DEFAULT_AUTO_RESUME_DELAY_SECONDS,
        ),
    )

    fun setSizePercent(value: Int) = update { copy(sizePercent = value) }

    fun setOpacityPercent(value: Int) = update { copy(opacityPercent = value) }

    fun setMovementEnabled(value: Boolean) = update { copy(movementEnabled = value) }

    fun setInteractionsEnabled(value: Boolean) = update { copy(interactionsEnabled = value) }

    fun setAnnouncementFrequency(value: MascotAnnouncementFrequency) {
        update { copy(announcementFrequency = value) }
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        update { copy(quietStartHour = startHour, quietEndHour = endHour) }
    }

    fun setAutoResumeDelaySeconds(value: Int) {
        update { copy(autoResumeDelaySeconds = value) }
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private inline fun update(transform: MascotAppearance.() -> MascotAppearance) {
        write(read().transform())
    }

    private fun write(value: MascotAppearance) {
        val normalized = MascotAppearance.normalized(
            sizePercent = value.sizePercent,
            opacityPercent = value.opacityPercent,
            movementEnabled = value.movementEnabled,
            interactionsEnabled = value.interactionsEnabled,
            announcementFrequency = value.announcementFrequency,
            quietStartHour = value.quietStartHour,
            quietEndHour = value.quietEndHour,
            autoResumeDelaySeconds = value.autoResumeDelaySeconds,
        )
        preferences.edit {
            putInt(KEY_SIZE_PERCENT, normalized.sizePercent)
            putInt(KEY_OPACITY_PERCENT, normalized.opacityPercent)
            putBoolean(KEY_MOVEMENT_ENABLED, normalized.movementEnabled)
            putBoolean(KEY_INTERACTIONS_ENABLED, normalized.interactionsEnabled)
            putString(
                KEY_ANNOUNCEMENT_FREQUENCY,
                normalized.announcementFrequency.storageValue,
            )
            putInt(KEY_QUIET_START_HOUR, normalized.quietStartHour)
            putInt(KEY_QUIET_END_HOUR, normalized.quietEndHour)
            putInt(KEY_AUTO_RESUME_DELAY_SECONDS, normalized.autoResumeDelaySeconds)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "mascot_appearance"
        const val KEY_SIZE_PERCENT = "size_percent"
        const val KEY_OPACITY_PERCENT = "opacity_percent"
        const val KEY_MOVEMENT_ENABLED = "movement_enabled"
        const val KEY_INTERACTIONS_ENABLED = "interactions_enabled"
        const val KEY_ANNOUNCEMENT_FREQUENCY = "announcement_frequency"
        const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        const val KEY_AUTO_RESUME_DELAY_SECONDS = "auto_resume_delay_seconds"
    }
}
