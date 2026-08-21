package com.example.todoapp

import android.Manifest
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
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    private var mascotSizePercent by mutableStateOf(MascotAppearance.DEFAULT_SIZE_PERCENT)
    private var mascotOpacityPercent by mutableStateOf(MascotAppearance.DEFAULT_OPACITY_PERCENT)
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
                    onRequestExactAlarmAccess = ::requestExactAlarmAccess,
                    onRequestFullScreenAlertAccess = ::requestFullScreenAlertAccess,
                    onOpenNotificationSettings = ::openNotificationSettings,
                    onOpenAppSettings = ::openAppSettings,
                    onRequestMascotOverlayAccess = ::requestMascotOverlayAccess,
                    onShowMascot = ::showMascot,
                    onHideMascot = ::hideMascot,
                    onMascotSizeChange = ::updateMascotSizePercent,
                    onMascotOpacityChange = ::updateMascotOpacityPercent,
                )
            }
        }

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
    }

    private fun updateMascotSizePercent(value: Int) {
        val appearance = MascotAppearance.normalized(value, mascotOpacityPercent)
        mascotSizePercent = appearance.sizePercent
        mascotAppearancePreferences.setSizePercent(appearance.sizePercent)
    }

    private fun updateMascotOpacityPercent(value: Int) {
        val appearance = MascotAppearance.normalized(mascotSizePercent, value)
        mascotOpacityPercent = appearance.opacityPercent
        mascotAppearancePreferences.setOpacityPercent(appearance.opacityPercent)
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
}
