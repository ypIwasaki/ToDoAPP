package com.example.todoapp.mascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.todoapp.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class MascotSpriteSet(
    val expressions: List<Bitmap>,
    val walkFrames: List<Bitmap>,
) {
    fun recycle() {
        (expressions + walkFrames).toSet().forEach { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}

internal data class MascotSpriteFrame(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal object MascotSpriteFactory {
    private const val FRONT_VIEW_RIGHT_RATIO = 0.385f
    private const val EXPRESSION_COLUMNS = 3
    private const val EXPRESSION_ROWS = 2
    private const val EXPRESSION_FRAME_COUNT = 6
    private const val WALK_COLUMNS = 6
    private const val WALK_ROWS = 1
    private const val WALK_FRAME_COUNT = 6
    private const val FRAME_PADDING_RATIO = 0.025f

    fun createAnimatedSprites(context: Context): MascotSpriteSet {
        var expressions: List<Bitmap> = emptyList()
        return try {
            expressions = decodeSpriteSheet(
                context = context,
                drawableId = R.drawable.mascot_expressions,
                columns = EXPRESSION_COLUMNS,
                rows = EXPRESSION_ROWS,
                frameCount = EXPRESSION_FRAME_COUNT,
            )
            val walkFrames = decodeSpriteSheet(
                context = context,
                drawableId = R.drawable.mascot_walk_cycle,
                columns = WALK_COLUMNS,
                rows = WALK_ROWS,
                frameCount = WALK_FRAME_COUNT,
            )
            MascotSpriteSet(expressions, walkFrames)
        } catch (_: RuntimeException) {
            expressions.forEach { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            val fallback = createFrontSprite(context)
            MascotSpriteSet(listOf(fallback), listOf(fallback))
        }
    }

    fun createFrontSprite(context: Context): Bitmap {
        val sheet = requireNotNull(
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.mascot_reference,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            ),
        ) { "Mascot reference image could not be decoded" }

        val cropWidth = (sheet.width * FRONT_VIEW_RIGHT_RATIO)
            .roundToInt()
            .coerceIn(1, sheet.width)
        val cropped = Bitmap.createBitmap(sheet, 0, 0, cropWidth, sheet.height)
        if (cropped !== sheet) sheet.recycle()

        val sprite = cropped.copy(Bitmap.Config.ARGB_8888, true)
        if (sprite !== cropped) cropped.recycle()

        val pixels = IntArray(sprite.width * sprite.height)
        sprite.getPixels(pixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
        for (index in pixels.indices) {
            if (isLavenderBackground(pixels[index])) {
                pixels[index] = Color.TRANSPARENT
            }
        }
        sprite.setPixels(pixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
        return sprite
    }

    internal fun isLavenderBackground(color: Int): Boolean {
        val red = color ushr 16 and 0xFF
        val green = color ushr 8 and 0xFF
        val blue = color and 0xFF
        return red >= 150 &&
            green >= 150 &&
            blue >= 170 &&
            blue - red >= 4 &&
            blue - green >= 4 &&
            abs(red - green) <= 28
    }

    internal fun isCheckerboardBackground(color: Int): Boolean {
        val alpha = color ushr 24 and 0xFF
        if (alpha == 0) return true
        val red = color ushr 16 and 0xFF
        val green = color ushr 8 and 0xFF
        val blue = color and 0xFF
        val darkest = min(red, min(green, blue))
        val lightest = max(red, max(green, blue))
        return darkest >= 228 && lightest - darkest <= 12
    }

    internal fun spriteFrame(
        index: Int,
        sheetWidth: Int,
        sheetHeight: Int,
        columns: Int,
        rows: Int,
    ): MascotSpriteFrame {
        require(columns > 0 && rows > 0)
        require(index in 0 until columns * rows)
        val frameWidth = sheetWidth / columns
        val frameHeight = sheetHeight / rows
        return MascotSpriteFrame(
            left = index % columns * frameWidth,
            top = index / columns * frameHeight,
            width = frameWidth,
            height = frameHeight,
        )
    }

    private fun decodeSpriteSheet(
        context: Context,
        drawableId: Int,
        columns: Int,
        rows: Int,
        frameCount: Int,
    ): List<Bitmap> {
        val decoded = requireNotNull(
            BitmapFactory.decodeResource(
                context.resources,
                drawableId,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                },
            ),
        ) { "Mascot sprite sheet could not be decoded" }
        val sheet = removeConnectedCheckerboardBackground(decoded)
        if (sheet !== decoded) decoded.recycle()
        return try {
            List(frameCount) { index ->
                val frame = spriteFrame(index, sheet.width, sheet.height, columns, rows)
                val cropped = Bitmap.createBitmap(
                    sheet,
                    frame.left,
                    frame.top,
                    frame.width,
                    frame.height,
                )
                val copy = cropped.copy(Bitmap.Config.ARGB_8888, true).also { copy ->
                    if (copy !== cropped) cropped.recycle()
                }
                cropTransparentMargins(copy)
            }
        } finally {
            sheet.recycle()
        }
    }

    private fun cropTransparentMargins(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        pixels.forEachIndexed { index, color ->
            if (Color.alpha(color) == 0) return@forEachIndexed
            val x = index % width
            val y = index / width
            minX = min(minX, x)
            minY = min(minY, y)
            maxX = max(maxX, x)
            maxY = max(maxY, y)
        }
        if (maxX < minX || maxY < minY) return source

        val padding = (max(width, height) * FRAME_PADDING_RATIO).roundToInt()
        val left = (minX - padding).coerceAtLeast(0)
        val top = (minY - padding).coerceAtLeast(0)
        val right = (maxX + padding + 1).coerceAtMost(width)
        val bottom = (maxY + padding + 1).coerceAtMost(height)
        if (left == 0 && top == 0 && right == width && bottom == height) return source

        return Bitmap.createBitmap(source, left, top, right - left, bottom - top).also {
            if (it !== source) source.recycle()
        }
    }

    private fun removeConnectedCheckerboardBackground(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        var queueStart = 0
        var queueEnd = 0
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun enqueue(x: Int, y: Int) {
            if (x !in 0 until width || y !in 0 until height) return
            val index = y * width + x
            if (visited[index] || !isCheckerboardBackground(pixels[index])) return
            visited[index] = true
            queue[queueEnd++] = index
        }

        for (x in 0 until width) {
            enqueue(x, 0)
            enqueue(x, height - 1)
        }
        for (y in 0 until height) {
            enqueue(0, y)
            enqueue(width - 1, y)
        }

        while (queueStart < queueEnd) {
            val index = queue[queueStart++]
            val x = index % width
            val y = index / width
            pixels[index] = Color.TRANSPARENT
            enqueue(x - 1, y)
            enqueue(x + 1, y)
            enqueue(x, y - 1)
            enqueue(x, y + 1)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
