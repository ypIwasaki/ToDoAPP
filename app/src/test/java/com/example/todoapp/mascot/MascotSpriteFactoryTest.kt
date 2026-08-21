package com.example.todoapp.mascot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotSpriteFactoryTest {
    @Test
    fun lavenderSheetBackgroundIsRemoved() {
        assertTrue(MascotSpriteFactory.isLavenderBackground(0xFFC1BED8.toInt()))
        assertTrue(MascotSpriteFactory.isLavenderBackground(0xFFD7D4E8.toInt()))
    }

    @Test
    fun characterColorsAndWhiteOutlineArePreserved() {
        assertFalse(MascotSpriteFactory.isLavenderBackground(0xFFFFFFFF.toInt()))
        assertFalse(MascotSpriteFactory.isLavenderBackground(0xFFFFDFA5.toInt()))
        assertFalse(MascotSpriteFactory.isLavenderBackground(0xFF41415F.toInt()))
        assertFalse(MascotSpriteFactory.isLavenderBackground(0xFF65B8BC.toInt()))
    }
}
