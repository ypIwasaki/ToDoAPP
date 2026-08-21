package com.example.todoapp.mascot

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

internal class MascotInteractionController(
    private val view: View,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onDragStart: () -> Unit,
    private val onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    private val onDragEnd: () -> Unit,
) : View.OnTouchListener {
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false
    private var longPressHandled = false
    private var enabled = true
    private val longPressRunnable = Runnable {
        if (!dragging) {
            longPressHandled = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongPress()
        }
    }

    init {
        view.isClickable = true
        view.isLongClickable = true
        view.setOnTouchListener(this)
    }

    override fun onTouch(touchedView: View, event: MotionEvent): Boolean {
        if (!enabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                dragging = false
                longPressHandled = false
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }

            MotionEvent.ACTION_MOVE -> {
                if (longPressHandled) return true
                if (!dragging && hypot(
                        (event.rawX - downRawX).toDouble(),
                        (event.rawY - downRawY).toDouble(),
                    ) >= touchSlop
                ) {
                    handler.removeCallbacks(longPressRunnable)
                    dragging = true
                    onDragStart()
                }
                if (dragging) {
                    onDrag(event.rawX - lastRawX, event.rawY - lastRawY)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                when {
                    dragging -> onDragEnd()
                    !longPressHandled -> {
                        touchedView.performClick()
                        onTap()
                    }
                }
                resetGesture()
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (dragging) onDragEnd()
                resetGesture()
            }
        }
        return true
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        view.isClickable = value
        view.isLongClickable = value
        if (!value) {
            handler.removeCallbacks(longPressRunnable)
            if (dragging) onDragEnd()
            resetGesture()
        }
    }

    fun release() {
        handler.removeCallbacks(longPressRunnable)
        view.setOnTouchListener(null)
        resetGesture()
    }

    private fun resetGesture() {
        dragging = false
        longPressHandled = false
    }
}
