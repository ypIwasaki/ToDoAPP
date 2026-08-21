package com.example.todoapp.mascot

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MascotSpriteFactoryInstrumentedTest {
    @Test
    fun generatedSpriteSheetsAreDecodedTrimmedAndMadeTransparent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sprites = MascotSpriteFactory.createAnimatedSprites(context)

        try {
            assertEquals(6, sprites.expressions.size)
            assertEquals(6, sprites.walkFrames.size)
            sprites.expressions.forEach { frame ->
                assertFrameHasTransparentMarginAndVisibleCharacter(frame)
                assertTrue(frame.width < 512 || frame.height < 512)
            }
            sprites.walkFrames.forEach { frame ->
                assertFrameHasTransparentMarginAndVisibleCharacter(frame)
                assertTrue(frame.width < 362 || frame.height < 724)
            }
        } finally {
            sprites.recycle()
        }
    }

    private fun assertFrameHasTransparentMarginAndVisibleCharacter(frame: Bitmap) {
        val corners = listOf(
            frame.getPixel(0, 0),
            frame.getPixel(frame.width - 1, 0),
            frame.getPixel(0, frame.height - 1),
            frame.getPixel(frame.width - 1, frame.height - 1),
        )
        assertTrue(corners.all { color -> Color.alpha(color) == 0 })

        val pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        assertTrue(pixels.any { color -> Color.alpha(color) > 0 })
    }
}
