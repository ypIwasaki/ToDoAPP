package com.example.todoapp.mascot

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

internal class MascotMovementController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val view: View,
    private val layoutParams: WindowManager.LayoutParams,
    private val onWindowError: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val random = Random.Default
    private var appearance = MascotAppearance.normalized(
        MascotAppearance.DEFAULT_SIZE_PERCENT,
        MascotAppearance.DEFAULT_OPACITY_PERCENT,
    )
    private var movementAnimator: ValueAnimator? = null
    private var started = false
    private var receiverRegistered = false
    private val moveRunnable = Runnable(::startNextMove)
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> stopMovement()
                Intent.ACTION_SCREEN_ON -> restartMovement(INITIAL_MOVE_DELAY_MILLIS)
            }
        }
    }

    fun prepareInitial(appearance: MascotAppearance) {
        this.appearance = appearance
        applyVisualSettings()
        val bounds = currentMovementBounds()
        layoutParams.x = bounds.maxX
        layoutParams.y = bounds.maxY
    }

    fun start() {
        if (started) return
        started = true
        registerScreenReceiver()
        restartMovement(INITIAL_MOVE_DELAY_MILLIS)
    }

    fun updateAppearance(appearance: MascotAppearance) {
        handler.post {
            if (!started) return@post
            this.appearance = appearance
            applyVisualSettings()
            val bounds = currentMovementBounds()
            val position = bounds.clamp(MascotPosition(layoutParams.x, layoutParams.y))
            layoutParams.x = position.x
            layoutParams.y = position.y
            if (updateViewLayout()) {
                restartMovement(SHORT_RESTART_DELAY_MILLIS)
            }
        }
    }

    fun onConfigurationChanged() {
        updateAppearance(appearance)
    }

    fun release() {
        started = false
        stopMovement()
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(screenStateReceiver) }
            receiverRegistered = false
        }
    }

    private fun applyVisualSettings() {
        view.alpha = appearance.alpha
        val area = currentSafeArea()
        val size = MascotMovementPlanner.fitWindowSize(
            requestedWidth = dp(appearance.scaledDimension(BASE_WIDTH_DP)),
            requestedHeight = dp(appearance.scaledDimension(BASE_HEIGHT_DP)),
            availableWidth = area.availableWidth,
            availableHeight = area.availableHeight,
        )
        layoutParams.width = size.width
        layoutParams.height = size.height
    }

    private fun startNextMove() {
        if (!canMove()) return
        val bounds = currentMovementBounds()
        val start = bounds.clamp(MascotPosition(layoutParams.x, layoutParams.y))
        val target = chooseTarget(bounds, start)
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = MascotMovementPlanner.durationMillis(
                start,
                target,
                context.resources.displayMetrics.density,
            )
            interpolator = LinearInterpolator()
        }
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            layoutParams.x = lerp(start.x, target.x, fraction)
            layoutParams.y = lerp(start.y, target.y, fraction)
            if (!updateViewLayout()) {
                stopMovement()
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (movementAnimator !== animator) return
                movementAnimator = null
                scheduleNextMove(randomPauseMillis())
            }
        })
        movementAnimator = animator
        animator.start()
    }

    private fun chooseTarget(
        bounds: MascotMovementBounds,
        current: MascotPosition,
    ): MascotPosition {
        var candidate = current
        repeat(TARGET_ATTEMPTS) {
            candidate = MascotPosition(
                x = randomCoordinate(bounds.minX, bounds.maxX),
                y = randomCoordinate(bounds.minY, bounds.maxY),
            )
            val distance = hypot(
                (candidate.x - current.x).toDouble(),
                (candidate.y - current.y).toDouble(),
            )
            if (distance >= dp(MIN_TRAVEL_DISTANCE_DP)) return candidate
        }
        return candidate
    }

    private fun restartMovement(delayMillis: Long) {
        stopMovement()
        scheduleNextMove(delayMillis)
    }

    private fun stopMovement() {
        handler.removeCallbacks(moveRunnable)
        val animator = movementAnimator
        movementAnimator = null
        animator?.cancel()
    }

    private fun scheduleNextMove(delayMillis: Long) {
        if (canMove()) {
            handler.postDelayed(moveRunnable, delayMillis)
        }
    }

    private fun canMove(): Boolean =
        started && appearance.movementEnabled && powerManager.isInteractive

    private fun updateViewLayout(): Boolean = try {
        windowManager.updateViewLayout(view, layoutParams)
        true
    } catch (_: IllegalArgumentException) {
        onWindowError()
        false
    } catch (_: SecurityException) {
        onWindowError()
        false
    }

    private fun currentMovementBounds(): MascotMovementBounds {
        val area = currentSafeArea()
        return MascotMovementPlanner.movementBounds(
            displayWidth = area.displayWidth,
            displayHeight = area.displayHeight,
            insetLeft = area.left,
            insetTop = area.top,
            insetRight = area.right,
            insetBottom = area.bottom,
            characterWidth = layoutParams.width,
            characterHeight = layoutParams.height,
        )
    }

    private fun currentSafeArea(): SafeArea {
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )
        val edgePadding = dp(EDGE_PADDING_DP)
        val left = insets.left + edgePadding
        val top = insets.top + edgePadding
        val right = insets.right + edgePadding
        val bottom = insets.bottom + edgePadding
        return SafeArea(
            displayWidth = metrics.bounds.width(),
            displayHeight = metrics.bounds.height(),
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun randomCoordinate(minimum: Int, maximum: Int): Int =
        if (maximum <= minimum) minimum else random.nextInt(minimum, maximum + 1)

    private fun randomPauseMillis(): Long =
        random.nextLong(MIN_PAUSE_MILLIS, MAX_PAUSE_MILLIS + 1)

    private fun lerp(start: Int, end: Int, fraction: Float): Int =
        (start + (end - start) * fraction).roundToInt()

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private data class SafeArea(
        val displayWidth: Int,
        val displayHeight: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val availableWidth: Int
            get() = (displayWidth - left - right).coerceAtLeast(1)
        val availableHeight: Int
            get() = (displayHeight - top - bottom).coerceAtLeast(1)
    }

    private companion object {
        const val BASE_WIDTH_DP = 184
        const val BASE_HEIGHT_DP = 330
        const val EDGE_PADDING_DP = 8
        const val MIN_TRAVEL_DISTANCE_DP = 72
        const val TARGET_ATTEMPTS = 8
        const val INITIAL_MOVE_DELAY_MILLIS = 1_000L
        const val SHORT_RESTART_DELAY_MILLIS = 400L
        const val MIN_PAUSE_MILLIS = 900L
        const val MAX_PAUSE_MILLIS = 2_600L
    }
}
