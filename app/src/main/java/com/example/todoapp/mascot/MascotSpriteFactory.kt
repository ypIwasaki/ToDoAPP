package com.example.todoapp.mascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.todoapp.R
import kotlin.math.abs
import kotlin.math.roundToInt

internal object MascotSpriteFactory {
    private const val FRONT_VIEW_RIGHT_RATIO = 0.385f

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
}
