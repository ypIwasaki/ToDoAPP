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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshReminderSystemStatus()
        rescheduleReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createReminderNotificationChannel(this)
        refreshReminderSystemStatus()
        setContent {
            TodoTheme {
                TodoApp(
                    viewModel = viewModel,
                    reminderSystemStatus = reminderSystemStatus,
                    onRequestExactAlarmAccess = ::requestExactAlarmAccess,
                    onRequestFullScreenAlertAccess = ::requestFullScreenAlertAccess,
                    onOpenNotificationSettings = ::openNotificationSettings,
                    onOpenAppSettings = ::openAppSettings,
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
        rescheduleReminders()
    }

    private fun refreshReminderSystemStatus() {
        reminderSystemStatus = readReminderSystemStatus(this)
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
}
