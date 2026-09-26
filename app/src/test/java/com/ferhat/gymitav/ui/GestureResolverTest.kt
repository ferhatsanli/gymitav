package com.ferhat.gymitav.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureResolverTest {
    @Test
    fun directionalSwipesMapToOneActionEach() {
        assertEquals(MainScreenAction.INCREASE_TARGET_SETS, resolveSwipeAction(0f, 80f, 36f))
        assertEquals(MainScreenAction.OPEN_SETTINGS, resolveSwipeAction(-80f, 0f, 36f))
        assertEquals(MainScreenAction.COMPLETE_SET, resolveSwipeAction(0f, -80f, 36f))
        assertEquals(MainScreenAction.BACK, resolveSwipeAction(80f, 0f, 36f))
    }

    @Test
    fun shortOrDiagonalDragDoesNotTriggerAnAction() {
        assertNull(resolveSwipeAction(0f, 25f, 36f))
        assertNull(resolveSwipeAction(50f, 50f, 36f))
    }

    @Test
    fun centerAndFourAnnularSectorsResolveIndependently() {
        val centerX = 100f
        val centerY = 100f
        val side = 200f

        assertEquals(MainScreenAction.TOGGLE_TIMER, resolveTapAction(100f, 100f, centerX, centerY, side))
        assertEquals(MainScreenAction.INCREASE_TARGET_SETS, resolveTapAction(100f, 25f, centerX, centerY, side))
        assertEquals(MainScreenAction.OPEN_SETTINGS, resolveTapAction(175f, 100f, centerX, centerY, side))
        assertEquals(MainScreenAction.COMPLETE_SET, resolveTapAction(100f, 175f, centerX, centerY, side))
        assertEquals(MainScreenAction.BACK, resolveTapAction(25f, 100f, centerX, centerY, side))
        assertNull(resolveTapAction(5f, 5f, centerX, centerY, side))
    }
}
