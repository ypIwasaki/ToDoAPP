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
import android.content.res.Configuration
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
import com.example.todoapp.TodoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlinx.coroutines.flow.asStateFlow

class MascotOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var appearancePreferences: MascotAppearancePreferences
    private lateinit var positionPreferences: MascotPositionPreferences
    private lateinit var messageHistory: MascotMessageHistoryStore
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val stateController = MascotStateController()
    private var spriteAnimator: MascotSpriteAnimator? = null
    private var interactionController: MascotInteractionController? = null
    private var auxiliaryController: MascotAuxiliaryOverlayController? = null
    private var mascotView: View? = null
    private var movementController: MascotMovementController? = null
    private var taskObserver: MascotTaskObserver? = null
    private val appearanceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            updateMascotSettings()
        }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        appearancePreferences = MascotAppearancePreferences(this)
        positionPreferences = MascotPositionPreferences(this)
        messageHistory = MascotMessageHistoryStore(this)
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
        } else {
            updateMascotSettings()
        }
        when (intent?.action) {
            ACTION_TASK_REMINDER ->
                showReminderTask(intent.getLongExtra(EXTRA_TASK_ID, INVALID_TASK_ID))
            ACTION_PERIODIC_ANNOUNCEMENT -> showPeriodicOverview()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        movementController?.onConfigurationChanged()
    }

    override fun onDestroy() {
        appearancePreferences.unregisterListener(appearanceChangeListener)
        taskObserver?.release()
        taskObserver = null
        interactionController?.release()
        interactionController = null
        auxiliaryController?.release()
        auxiliaryController = null
        movementController?.release()
        movementController = null
        spriteAnimator?.release()
        spriteAnimator = null
        mascotView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        mascotView = null
        _isRunning.value = false
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun showMascot() {
        val appearance = appearancePreferences.read()
        val sprites = MascotSpriteFactory.createAnimatedSprites(this)
        val view = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.mascot_content_description)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val animator = MascotSpriteAnimator(
            view = view,
            sprites = sprites,
        )
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setFitInsetsTypes(0)
        }
        val controller = MascotMovementController(
            context = this,
            windowManager = windowManager,
            view = view,
            layoutParams = params,
            onWalkingStarted = { movingRight ->
                stateController.transitionTo(MascotState.WALKING)
                animator.startWalking(movingRight)
            },
            onWalkingStopped = {
                animator.stopWalking()
                if (stateController.current == MascotState.WALKING) {
                    stateController.transitionTo(MascotState.IDLE)
                }
            },
            onScreenStateChanged = { screenOn ->
                stateController.transitionTo(
                    if (screenOn) MascotState.IDLE else MascotState.SCREEN_OFF,
                )
            },
            onUserPositionChanged = positionPreferences::save,
            onPlacementChanged = { placement ->
                auxiliaryController?.updateAnchor(placement)
            },
            onWindowError = ::stopSelf,
        ).apply {
            prepareInitial(appearance, positionPreferences.read())
        }
        val auxiliary = MascotAuxiliaryOverlayController(
            context = this,
            windowManager = windowManager,
            onOpenApp = { MascotAppLauncher.open(this) },
            onToggleMovement = appearancePreferences::setMovementEnabled,
            onHideMascot = ::stopSelf,
            onShown = {
                controller.pauseForInteraction()
                stateController.transitionTo(MascotState.SPEAKING)
            },
            onDismissed = {
                stateController.transitionTo(MascotState.IDLE)
                controller.resumeAfterInteraction()
            },
            onWindowError = ::stopSelf,
        )
        val interaction = MascotInteractionController(
            view = view,
            onTap = {
                auxiliary.dismiss()
                MascotAppLauncher.open(this)
            },
            onLongPress = {
                auxiliary.showMenu(appearancePreferences.read().movementEnabled)
            },
            onDragStart = {
                auxiliary.dismiss(resumeMovement = false)
                controller.beginUserDrag()
                stateController.transitionTo(MascotState.DRAGGING)
            },
            onDrag = controller::dragBy,
            onDragEnd = {
                controller.endUserDrag(
                    appearancePreferences.read().autoResumeDelaySeconds * 1_000L,
                )
                stateController.transitionTo(MascotState.IDLE)
            },
        ).apply {
            setEnabled(appearance.interactionsEnabled)
        }

        try {
            windowManager.addView(view, params)
            mascotView = view
            movementController = controller
            spriteAnimator = animator
            interactionController = interaction
            auxiliaryController = auxiliary
            auxiliary.updateAnchor(
                MascotWindowPlacement(params.x, params.y, params.width, params.height),
            )
            _isRunning.value = true
            controller.start()
            taskObserver = MascotTaskObserver(
                repository = (application as TodoApplication).taskRepository,
                scope = serviceScope,
                onEvent = ::handleTaskEvent,
            ).also(MascotTaskObserver::start)
        } catch (_: SecurityException) {
            interaction.release()
            auxiliary.release()
            animator.release()
            stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            interaction.release()
            auxiliary.release()
            animator.release()
            stopSelf()
        }
    }

    private fun handleTaskEvent(event: MascotTaskEvent) {
        if (stateController.current == MascotState.SCREEN_OFF) return
        val appearance = appearancePreferences.read()
        val proactiveEvent = event.kind == MascotTaskEventKind.OVERVIEW ||
            event.kind == MascotTaskEventKind.OVERDUE ||
            event.kind == MascotTaskEventKind.REMINDER
        if (
            proactiveEvent &&
            (
                appearance.announcementFrequency == MascotAnnouncementFrequency.OFF ||
                    appearance.isQuietHour(LocalTime.now().hour)
                )
        ) return
        if (!messageHistory.shouldShow(event.key)) return
        spriteAnimator?.showExpression(event.expression)
        auxiliaryController?.showMessage(
            message = event.message,
            actions = actionsFor(event),
            durationMillis = if (event.kind == MascotTaskEventKind.REMINDER) {
                REMINDER_MESSAGE_DURATION_MILLIS
            } else {
                DEFAULT_MESSAGE_DURATION_MILLIS
            },
        )
    }

    private fun actionsFor(event: MascotTaskEvent): List<MascotBubbleAction> {
        val taskId = event.taskId ?: return emptyList()
        return if (
            event.kind == MascotTaskEventKind.COMPLETED ||
            event.kind == MascotTaskEventKind.ALL_COMPLETED
        ) {
            listOf(
                MascotBubbleAction(getString(R.string.mascot_action_undo)) {
                    serviceScope.launch {
                        (application as TodoApplication).taskRepository
                            .setTaskCompleted(taskId, false)
                    }
                },
                MascotBubbleAction(getString(R.string.mascot_action_open)) {
                    MascotAppLauncher.open(this, taskId)
                },
            )
        } else {
            listOf(
                MascotBubbleAction(getString(R.string.mascot_action_open)) {
                    MascotAppLauncher.open(this, taskId)
                },
                MascotBubbleAction(getString(R.string.mascot_action_complete)) {
                    serviceScope.launch {
                        (application as TodoApplication).taskRepository
                            .setTaskCompleted(taskId, true)
                    }
                },
                MascotBubbleAction(getString(R.string.mascot_action_snooze_10)) {
                    snoozeTask(taskId, 10)
                },
                MascotBubbleAction(getString(R.string.mascot_action_snooze_60)) {
                    snoozeTask(taskId, 60)
                },
            )
        }
    }

    private fun snoozeTask(taskId: Long, minutes: Int) {
        serviceScope.launch {
            val scheduled = (application as TodoApplication).taskRepository.snoozeTask(
                taskId = taskId,
                delayMillis = minutes * 60_000L,
            )
            if (scheduled && stateController.current != MascotState.SCREEN_OFF) {
                spriteAnimator?.showExpression(MascotExpression.NORMAL)
                auxiliaryController?.showMessage(
                    message = getString(R.string.mascot_snooze_confirmation, minutes),
                )
            }
        }
    }

    private fun showReminderTask(taskId: Long) {
        if (taskId == INVALID_TASK_ID) return
        serviceScope.launch {
            val task = (application as TodoApplication).taskRepository.getTask(taskId)?.task
                ?: return@launch
            handleTaskEvent(MascotTaskEventPlanner.reminder(task))
        }
    }

    private fun showPeriodicOverview() {
        serviceScope.launch {
            val tasks = (application as TodoApplication).taskRepository
                .observeTasks()
                .first()
                .map { it.task }
            handleTaskEvent(MascotTaskEventPlanner.overview(tasks, System.currentTimeMillis()))
        }
    }

    private fun updateMascotSettings() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val appearance = appearancePreferences.read()
        interactionController?.setEnabled(appearance.interactionsEnabled)
        if (!appearance.interactionsEnabled) auxiliaryController?.dismiss()
        movementController?.updateAppearance(appearance)
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

    companion object {
        private const val CHANNEL_ID = "mascot_overlay_status"
        private const val NOTIFICATION_ID = 10_001
        private const val ACTION_SHOW = "com.example.todoapp.mascot.SHOW"
        private const val ACTION_HIDE = "com.example.todoapp.mascot.HIDE"
        private const val ACTION_TASK_REMINDER = "com.example.todoapp.mascot.TASK_REMINDER"
        private const val ACTION_PERIODIC_ANNOUNCEMENT =
            "com.example.todoapp.mascot.PERIODIC_ANNOUNCEMENT"
        private const val EXTRA_TASK_ID = "task_id"
        private const val INVALID_TASK_ID = -1L
         private const val DEFAULT_MESSAGE_DURATION_MILLIS = 7_000L
        private const val REMINDER_MESSAGE_DURATION_MILLIS = 12_000L

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun showIntent(context: Context): Intent =
            Intent(context, MascotOverlayService::class.java).setAction(ACTION_SHOW)

        fun requestPeriodicAnnouncement(context: Context) {
            if (!_isRunning.value) return
            runCatching {
                context.startService(
                    Intent(context, MascotOverlayService::class.java)
                        .setAction(ACTION_PERIODIC_ANNOUNCEMENT),
                )
            }
        }

        fun notifyTaskReminder(context: Context, taskId: Long) {
            if (!_isRunning.value) return
            runCatching {
                context.startService(
                    Intent(context, MascotOverlayService::class.java)
                        .setAction(ACTION_TASK_REMINDER)
                        .putExtra(EXTRA_TASK_ID, taskId),
                )
            }
        }
    }
}
