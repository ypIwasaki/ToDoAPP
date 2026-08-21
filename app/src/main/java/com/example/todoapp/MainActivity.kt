package com.example.todoapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoapp.mascot.MascotAnnouncementFrequency
import com.example.todoapp.mascot.MascotAnnouncementScheduler
import com.example.todoapp.mascot.MascotAppearance
import com.example.todoapp.mascot.MascotAppearancePreferences
import com.example.todoapp.mascot.MascotOverlayService
import com.example.todoapp.notification.ReminderSystemStatus
import com.example.todoapp.notification.createReminderNotificationChannel
import com.example.todoapp.notification.readReminderSystemStatus
import com.example.todoapp.ui.TodoApp
import com.example.todoapp.ui.TodoViewModel
import com.example.todoapp.ui.theme.TodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels {
        TodoViewModel.Factory(application)
    }

    private var reminderSystemStatus by mutableStateOf(ReminderSystemStatus())
    private lateinit var mascotAppearancePreferences: MascotAppearancePreferences
    private var mascotOverlayAllowed by mutableStateOf(false)
    private var mascotSizePercent by mutableIntStateOf(MascotAppearance.DEFAULT_SIZE_PERCENT)
    private var mascotOpacityPercent by mutableIntStateOf(MascotAppearance.DEFAULT_OPACITY_PERCENT)
    private var mascotMovementEnabled by mutableStateOf(MascotAppearance.DEFAULT_MOVEMENT_ENABLED)
    private var mascotInteractionsEnabled by mutableStateOf(
        MascotAppearance.DEFAULT_INTERACTIONS_ENABLED,
    )
    private var mascotAnnouncementFrequency by mutableStateOf(
        MascotAppearance.DEFAULT_ANNOUNCEMENT_FREQUENCY,
    )
    private var mascotQuietStartHour by mutableIntStateOf(MascotAppearance.DEFAULT_QUIET_START_HOUR)
    private var mascotQuietEndHour by mutableIntStateOf(MascotAppearance.DEFAULT_QUIET_END_HOUR)
    private var mascotAutoResumeDelaySeconds by mutableIntStateOf(
        MascotAppearance.DEFAULT_AUTO_RESUME_DELAY_SECONDS,
    )
    private var startMascotAfterPermissionRequest = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshReminderSystemStatus()
        rescheduleReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mascotAppearancePreferences = MascotAppearancePreferences(this)
        enableEdgeToEdge()
        createReminderNotificationChannel(this)
        refreshReminderSystemStatus()
        refreshMascotOverlayAccess()
        refreshMascotAppearance()
        setContent {
            val mascotVisible by MascotOverlayService.isRunning.collectAsStateWithLifecycle()
            TodoTheme {
                TodoApp(
                    viewModel = viewModel,
                    reminderSystemStatus = reminderSystemStatus,
                    mascotOverlayAllowed = mascotOverlayAllowed,
                    mascotVisible = mascotVisible,
                    mascotSizePercent = mascotSizePercent,
                    mascotOpacityPercent = mascotOpacityPercent,
                    mascotMovementEnabled = mascotMovementEnabled,
                    mascotInteractionsEnabled = mascotInteractionsEnabled,
                    mascotAnnouncementFrequency = mascotAnnouncementFrequency,
                    mascotQuietStartHour = mascotQuietStartHour,
                    mascotQuietEndHour = mascotQuietEndHour,
                    mascotAutoResumeDelaySeconds = mascotAutoResumeDelaySeconds,
                    onRequestExactAlarmAccess = ::requestExactAlarmAccess,
                    onRequestFullScreenAlertAccess = ::requestFullScreenAlertAccess,
                    onOpenNotificationSettings = ::openNotificationSettings,
                    onOpenAppSettings = ::openAppSettings,
                    onRequestMascotOverlayAccess = ::requestMascotOverlayAccess,
                    onShowMascot = ::showMascot,
                    onHideMascot = ::hideMascot,
                    onMascotSizeChange = ::updateMascotSizePercent,
                    onMascotOpacityChange = ::updateMascotOpacityPercent,
                    onMascotMovementEnabledChange = ::updateMascotMovementEnabled,
                    onMascotInteractionsEnabledChange = ::updateMascotInteractionsEnabled,
                    onMascotAnnouncementFrequencyChange = ::updateMascotAnnouncementFrequency,
                    onMascotQuietStartHourChange = ::updateMascotQuietStartHour,
                    onMascotQuietEndHourChange = ::updateMascotQuietEndHour,
                    onMascotAutoResumeDelayChange = ::updateMascotAutoResumeDelay,
                )
            }
        }

        handleLaunchIntent(intent)

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshReminderSystemStatus()
        refreshMascotOverlayAccess()
        refreshMascotAppearance()
        if (mascotOverlayAllowed && startMascotAfterPermissionRequest) {
            startMascotAfterPermissionRequest = false
            showMascot()
        }
        rescheduleReminders()
    }

    private fun handleLaunchIntent(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_FROM_MASCOT, false)) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) {
            viewModel.selectMainSection(com.example.todoapp.ui.MainSection.TASKS)
        } else {
            viewModel.startEdit(taskId)
        }
    }

    private fun refreshReminderSystemStatus() {
        reminderSystemStatus = readReminderSystemStatus(this)
    }

    private fun refreshMascotOverlayAccess() {
        mascotOverlayAllowed = Settings.canDrawOverlays(this)
    }

    private fun refreshMascotAppearance() {
        val appearance = mascotAppearancePreferences.read()
        mascotSizePercent = appearance.sizePercent
        mascotOpacityPercent = appearance.opacityPercent
        mascotMovementEnabled = appearance.movementEnabled
        mascotInteractionsEnabled = appearance.interactionsEnabled
        mascotAnnouncementFrequency = appearance.announcementFrequency
        mascotQuietStartHour = appearance.quietStartHour
        mascotQuietEndHour = appearance.quietEndHour
        mascotAutoResumeDelaySeconds = appearance.autoResumeDelaySeconds
    }

    private fun updateMascotSizePercent(value: Int) {
        val appearance = MascotAppearance.normalized(
            sizePercent = value,
            opacityPercent = mascotOpacityPercent,
            movementEnabled = mascotMovementEnabled,
        )
        mascotSizePercent = appearance.sizePercent
        mascotAppearancePreferences.setSizePercent(appearance.sizePercent)
    }

    private fun updateMascotOpacityPercent(value: Int) {
        val appearance = MascotAppearance.normalized(
            sizePercent = mascotSizePercent,
            opacityPercent = value,
            movementEnabled = mascotMovementEnabled,
        )
        mascotOpacityPercent = appearance.opacityPercent
        mascotAppearancePreferences.setOpacityPercent(appearance.opacityPercent)
    }

    private fun updateMascotMovementEnabled(value: Boolean) {
        mascotMovementEnabled = value
        mascotAppearancePreferences.setMovementEnabled(value)
    }

    private fun updateMascotInteractionsEnabled(value: Boolean) {
        mascotInteractionsEnabled = value
        mascotAppearancePreferences.setInteractionsEnabled(value)
    }

    private fun updateMascotAnnouncementFrequency(value: MascotAnnouncementFrequency) {
        mascotAnnouncementFrequency = value
        mascotAppearancePreferences.setAnnouncementFrequency(value)
        MascotAnnouncementScheduler.update(this, value)
    }

    private fun updateMascotQuietStartHour(value: Int) {
        mascotQuietStartHour = value.coerceIn(0, 23)
        mascotAppearancePreferences.setQuietHours(
            startHour = mascotQuietStartHour,
            endHour = mascotQuietEndHour,
        )
    }

    private fun updateMascotQuietEndHour(value: Int) {
        mascotQuietEndHour = value.coerceIn(0, 23)
        mascotAppearancePreferences.setQuietHours(
            startHour = mascotQuietStartHour,
            endHour = mascotQuietEndHour,
        )
    }

    private fun updateMascotAutoResumeDelay(value: Int) {
        mascotAutoResumeDelaySeconds = value.coerceIn(
            MascotAppearance.MIN_AUTO_RESUME_DELAY_SECONDS,
            MascotAppearance.MAX_AUTO_RESUME_DELAY_SECONDS,
        )
        mascotAppearancePreferences.setAutoResumeDelaySeconds(mascotAutoResumeDelaySeconds)
    }

    private fun rescheduleReminders() {
        lifecycleScope.launch {
            (application as TodoApplication).taskRepository.rescheduleAll()
        }
    }

    private fun requestExactAlarmAccess() {
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun requestFullScreenAlertAccess() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun requestMascotOverlayAccess() {
        startMascotAfterPermissionRequest = true
        val appSpecificIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri(),
        )
        runCatching { startActivity(appSpecificIntent) }
            .getOrElse { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
    }

    private fun showMascot() {
        if (!Settings.canDrawOverlays(this)) {
            requestMascotOverlayAccess()
            return
        }
        ContextCompat.startForegroundService(
            this,
            MascotOverlayService.showIntent(this),
        )
    }

    private fun hideMascot() {
        stopService(Intent(this, MascotOverlayService::class.java))
    }

    companion object {
        private const val EXTRA_FROM_MASCOT = "from_mascot"
        private const val EXTRA_TASK_ID = "mascot_task_id"
        private const val INVALID_TASK_ID = -1L

        fun createLaunchIntent(context: Context, taskId: Long? = null): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_FROM_MASCOT, true)
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            }
    }
}
