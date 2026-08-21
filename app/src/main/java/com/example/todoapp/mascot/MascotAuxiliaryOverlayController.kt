package com.example.todoapp.mascot

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.todoapp.R
import kotlin.math.roundToInt

internal data class MascotWindowPlacement(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal data class MascotBubbleAction(
    val label: String,
    val onClick: () -> Unit,
)

internal class MascotAuxiliaryOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onOpenApp: () -> Unit,
    private val onToggleMovement: (Boolean) -> Unit,
    private val onHideMascot: () -> Unit,
    private val onShown: () -> Unit,
    private val onDismissed: () -> Unit,
    private val onWindowError: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var anchor = MascotWindowPlacement(0, 0, 1, 1)
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var notifyOnDismiss = false
    private val dismissRunnable = Runnable(::dismiss)

    fun updateAnchor(placement: MascotWindowPlacement) {
        anchor = placement
        val view = overlayView ?: return
        val params = overlayParams ?: return
        positionOverlay(view, params)
        updateLayout(view, params)
    }

    fun showMenu(movementEnabled: Boolean) {
        val container = createContainer().apply {
            orientation = LinearLayout.VERTICAL
            addView(menuButton(context.getString(R.string.mascot_menu_open_app)) {
                dismiss()
                onOpenApp()
            })
            addView(
                menuButton(
                    context.getString(
                        if (movementEnabled) {
                            R.string.mascot_menu_pause_movement
                        } else {
                            R.string.mascot_menu_resume_movement
                        },
                    ),
                ) {
                    dismiss()
                    onToggleMovement(!movementEnabled)
                },
            )
            addView(menuButton(context.getString(R.string.mascot_menu_hide)) {
                dismiss(resumeMovement = false)
                onHideMascot()
            })
        }
        show(container, MENU_WIDTH_DP, MENU_TIMEOUT_MILLIS)
    }

    fun showMessage(
        message: String,
        actions: List<MascotBubbleAction> = emptyList(),
        durationMillis: Long = DEFAULT_MESSAGE_DURATION_MILLIS,
    ) {
        val container = createContainer().apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = message
                textSize = 16f
                setTextColor(foregroundColor())
                setLineSpacing(0f, 1.08f)
            })
            actions.take(MAX_ACTIONS).chunked(ACTIONS_PER_ROW).forEach { rowActions ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(8), 0, 0)
                    rowActions.forEach { action ->
                        addView(menuButton(action.label) {
                            dismiss()
                            action.onClick()
                        })
                    }
                })
            }
        }
        show(container, BUBBLE_WIDTH_DP, durationMillis)
    }

    fun dismiss(resumeMovement: Boolean = true) {
        handler.removeCallbacks(dismissRunnable)
        val view = overlayView ?: return
        overlayView = null
        overlayParams = null
        runCatching { windowManager.removeView(view) }
        if (notifyOnDismiss && resumeMovement) onDismissed()
        notifyOnDismiss = false
    }

    fun release() {
        dismiss(resumeMovement = false)
    }

    private fun show(view: View, widthDp: Int, durationMillis: Long) {
        dismiss(resumeMovement = false)
        val params = WindowManager.LayoutParams(
            dp(widthDp),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setFitInsetsTypes(0)
        }
        positionOverlay(view, params)
        try {
            windowManager.addView(view, params)
            overlayView = view
            overlayParams = params
            notifyOnDismiss = true
            onShown()
            handler.postDelayed(
                dismissRunnable,
                durationMillis.coerceAtLeast(MIN_MESSAGE_DURATION_MILLIS),
            )
        } catch (_: SecurityException) {
            onWindowError()
        } catch (_: WindowManager.BadTokenException) {
            onWindowError()
        }
    }

    private fun positionOverlay(view: View, params: WindowManager.LayoutParams) {
        val safeArea = safeArea()
        val width = params.width.coerceAtLeast(dp(MENU_WIDTH_DP))
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(safeArea.height, View.MeasureSpec.AT_MOST),
        )
        val height = view.measuredHeight.coerceAtLeast(dp(48))
        val gap = dp(8)
        val rightX = anchor.x + anchor.width + gap
        val leftX = anchor.x - width - gap
        params.x = when {
            rightX + width <= safeArea.right -> rightX
            leftX >= safeArea.left -> leftX
            else -> anchor.x.coerceIn(
                safeArea.left,
                (safeArea.right - width).coerceAtLeast(safeArea.left),
            )
        }
        val preferredAbove = anchor.y - height - gap
        params.y = when {
            preferredAbove >= safeArea.top -> preferredAbove
            anchor.y + anchor.height + gap + height <= safeArea.bottom ->
                anchor.y + anchor.height + gap
            else -> anchor.y.coerceIn(
                safeArea.top,
                (safeArea.bottom - height).coerceAtLeast(safeArea.top),
            )
        }
    }

    private fun updateLayout(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            onWindowError()
        } catch (_: SecurityException) {
            onWindowError()
        }
    }

    private fun createContainer(): LinearLayout = LinearLayout(context).apply {
        setPadding(dp(14), dp(12), dp(14), dp(12))
        elevation = dp(8).toFloat()
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(backgroundColor())
            setStroke(dp(1), borderColor())
        }
    }

    private fun menuButton(label: String, onClick: () -> Unit): Button = Button(context).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = dp(40)
        minimumHeight = dp(40)
        setTextColor(foregroundColor())
        backgroundTintList = ColorStateList.valueOf(buttonColor())
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            marginEnd = dp(4)
        }
        setOnClickListener { onClick() }
    }

    private fun safeArea(): SafeArea {
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )
        val padding = dp(8)
        return SafeArea(
            left = insets.left + padding,
            top = insets.top + padding,
            right = metrics.bounds.width() - insets.right - padding,
            bottom = metrics.bounds.height() - insets.bottom - padding,
        )
    }

    private fun isNightMode(): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    private fun backgroundColor(): Int =
        if (isNightMode()) Color.rgb(43, 42, 51) else Color.rgb(255, 251, 240)

    private fun foregroundColor(): Int =
        if (isNightMode()) Color.WHITE else Color.rgb(45, 40, 35)

    private fun borderColor(): Int =
        if (isNightMode()) Color.rgb(92, 88, 110) else Color.rgb(215, 190, 142)

    private fun buttonColor(): Int =
        if (isNightMode()) Color.rgb(68, 65, 82) else Color.rgb(250, 231, 190)

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private data class SafeArea(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val height: Int
            get() = (bottom - top).coerceAtLeast(1)
    }

    private companion object {
        const val MENU_WIDTH_DP = 224
        const val BUBBLE_WIDTH_DP = 300
        const val MAX_ACTIONS = 4
        const val ACTIONS_PER_ROW = 2
        const val MENU_TIMEOUT_MILLIS = 8_000L
        const val DEFAULT_MESSAGE_DURATION_MILLIS = 7_000L
        const val MIN_MESSAGE_DURATION_MILLIS = 1_000L
    }
}
