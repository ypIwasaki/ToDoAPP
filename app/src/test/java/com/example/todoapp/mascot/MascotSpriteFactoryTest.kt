package com.example.todoapp.mascot

import org.junit.Assert.assertEquals
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

    @Test
    fun generatedSheetCheckerboardIsDetectedWithoutRemovingCharacterColors() {
        assertTrue(MascotSpriteFactory.isCheckerboardBackground(0xFFFFFFFF.toInt()))
        assertTrue(MascotSpriteFactory.isCheckerboardBackground(0xFFF2F2F2.toInt()))
        assertFalse(MascotSpriteFactory.isCheckerboardBackground(0xFFFFDFA5.toInt()))
        assertFalse(MascotSpriteFactory.isCheckerboardBackground(0xFF41415F.toInt()))
        assertFalse(MascotSpriteFactory.isCheckerboardBackground(0xFF65B8BC.toInt()))
    }

    @Test
    fun expressionAndWalkSheetsProduceExpectedFrameBounds() {
        assertEquals(
            MascotSpriteFrame(left = 1_024, top = 512, width = 512, height = 512),
            MascotSpriteFactory.spriteFrame(
                index = 5,
                sheetWidth = 1_536,
                sheetHeight = 1_024,
                columns = 3,
                rows = 2,
            ),
        )
        assertEquals(
            MascotSpriteFrame(left = 1_810, top = 0, width = 362, height = 724),
            MascotSpriteFactory.spriteFrame(
                index = 5,
                sheetWidth = 2_172,
                sheetHeight = 724,
                columns = 6,
                rows = 1,
            ),
        )
    }
}
