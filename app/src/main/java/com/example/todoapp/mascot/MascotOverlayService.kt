package com.example.todoapp.mascot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.todoapp.MainActivity
import com.example.todoapp.R
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MascotOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var appearancePreferences: MascotAppearancePreferences
    private var mascotView: View? = null
    private var mascotLayoutParams: WindowManager.LayoutParams? = null
    private val appearanceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            updateMascotAppearance()
        }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        appearancePreferences = MascotAppearancePreferences(this)
        appearancePreferences.registerListener(appearanceChangeListener)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (mascotView == null) {
            showMascot()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        appearancePreferences.unregisterListener(appearanceChangeListener)
        mascotView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        mascotView = null
        mascotLayoutParams = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun showMascot() {
        val appearance = appearancePreferences.read()
        val view = ImageView(this).apply {
            setImageBitmap(MascotSpriteFactory.createFrontSprite(this@MascotOverlayService))
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = appearance.alpha
            contentDescription = getString(R.string.mascot_content_description)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val params = WindowManager.LayoutParams(
            dp(appearance.scaledDimension(BASE_WIDTH_DP)),
            dp(appearance.scaledDimension(BASE_HEIGHT_DP)),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(12)
            y = dp(80)
        }

        try {
            windowManager.addView(view, params)
            mascotView = view
            mascotLayoutParams = params
            _isRunning.value = true
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            stopSelf()
        }
    }

    private fun updateMascotAppearance() {
        val view = mascotView ?: return
        val params = mascotLayoutParams ?: return
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val appearance = appearancePreferences.read()
        view.alpha = appearance.alpha
        params.width = dp(appearance.scaledDimension(BASE_WIDTH_DP))
        params.height = dp(appearance.scaledDimension(BASE_HEIGHT_DP))
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            stopSelf()
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun startAsForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hideIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MascotOverlayService::class.java).setAction(ACTION_HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.mascot_overlay_notification_title))
            .setContentText(getString(R.string.mascot_overlay_notification_text))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.mascot_overlay_stop), hideIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.mascot_overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.mascot_overlay_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val CHANNEL_ID = "mascot_overlay_status"
        private const val NOTIFICATION_ID = 10_001
        private const val BASE_WIDTH_DP = 184
        private const val BASE_HEIGHT_DP = 330
        private const val ACTION_SHOW = "com.example.todoapp.mascot.SHOW"
        private const val ACTION_HIDE = "com.example.todoapp.mascot.HIDE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun showIntent(context: Context): Intent =
            Intent(context, MascotOverlayService::class.java).setAction(ACTION_SHOW)
    }
}
