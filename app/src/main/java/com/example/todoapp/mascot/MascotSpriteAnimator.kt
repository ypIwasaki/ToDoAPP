package com.example.todoapp.mascot

import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import kotlin.random.Random

internal class MascotSpriteAnimator(
    private val view: ImageView,
    private val sprites: MascotSpriteSet,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random.Default
    private var walkFrameIndex = 0
    private var lastExpressionIndex = 0
    private var walking = false
    private val nextWalkFrame = object : Runnable {
        override fun run() {
            if (!walking) return
            walkFrameIndex = (walkFrameIndex + 1) % sprites.walkFrames.size
            view.setImageBitmap(sprites.walkFrames[walkFrameIndex])
            handler.postDelayed(this, WALK_FRAME_DURATION_MILLIS)
        }
    }

    init {
        showExpression(0)
    }

    fun startWalking(movingRight: Boolean) {
        handler.removeCallbacks(nextWalkFrame)
        walking = true
        walkFrameIndex = 0
        view.scaleX = if (movingRight) -1f else 1f
        view.setImageBitmap(sprites.walkFrames[walkFrameIndex])
        if (sprites.walkFrames.size > 1) {
            handler.postDelayed(nextWalkFrame, WALK_FRAME_DURATION_MILLIS)
        }
    }

    fun stopWalking() {
        handler.removeCallbacks(nextWalkFrame)
        walking = false
        view.scaleX = 1f
        showExpression(nextExpressionIndex())
    }

    fun showExpression(expression: MascotExpression) {
        handler.removeCallbacks(nextWalkFrame)
        walking = false
        view.scaleX = 1f
        showExpression(expression.frameIndex)
    }

    fun release() {
        walking = false
        handler.removeCallbacks(nextWalkFrame)
        view.setImageDrawable(null)
        sprites.recycle()
    }

    private fun showExpression(index: Int) {
        lastExpressionIndex = index.coerceIn(sprites.expressions.indices)
        view.setImageBitmap(sprites.expressions[lastExpressionIndex])
    }

    private fun nextExpressionIndex(): Int {
        if (sprites.expressions.size <= 1) return 0
        val offset = random.nextInt(1, sprites.expressions.size)
        return (lastExpressionIndex + offset) % sprites.expressions.size
    }

    private companion object {
        const val WALK_FRAME_DURATION_MILLIS = 150L
    }
}
