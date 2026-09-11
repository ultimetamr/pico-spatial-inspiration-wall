package com.spatialapps.inspirationwall.domain

import com.spatialapps.inspirationwall.data.CardEntity
import com.spatialapps.inspirationwall.data.CardTransform
import com.spatialapps.inspirationwall.data.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLayoutEngineTest {
    private fun card(id: String, z: Int = 0) = CardEntity(
        id = id, wallId = "w", groupId = "g", type = CardType.TEXT.name,
        title = "t", content = "c", x = 0f, y = 0f, width = 200f, height = 120f, zIndex = z,
    )

    @Test fun cullingKeepsPartiallyVisibleCardsAndRejectsDistantCards() {
        val card = card("a")
        assertTrue(CardLayoutEngine.isVisible(card, CardTransform(-100f, 40f, 1f, 0f), 900f, 600f))
        assertFalse(CardLayoutEngine.isVisible(card, CardTransform(1400f, 40f, 1f, 0f), 900f, 600f))
    }

    @Test fun scaleIsClampedToInteractionContract() {
        assertEquals(.55f, CardLayoutEngine.clamped(CardTransform(0f, 0f, .1f, 0f)).scale)
        assertEquals(2.2f, CardLayoutEngine.clamped(CardTransform(0f, 0f, 5f, 0f)).scale)
    }

    @Test fun transformDeltasAccumulateAcrossFramesWithoutResettingTheGesture() {
        val first = CardLayoutEngine.applyDelta(CardTransform(10f, 20f, 1f, 2f), 4f, -3f, 1.1f, 5f)
        val second = CardLayoutEngine.applyDelta(first, 6f, 8f, 1.2f, -2f)

        assertEquals(20f, second.x)
        assertEquals(25f, second.y)
        assertEquals(1.32f, second.scale, .001f)
        assertEquals(5f, second.rotation)
    }

    @Test fun cardsAreKeptFullyInsideViewportAfterDraggingPastEveryEdge() {
        val card = card("bounded")
        val viewport = CardViewport(widthPx = 900f, heightPx = 600f, density = 1f)

        val topLeft = CardLayoutEngine.constrainToViewport(card, CardTransform(-500f, -300f, 1f, 0f), viewport)
        val bottomRight = CardLayoutEngine.constrainToViewport(card, CardTransform(1200f, 900f, 1f, 0f), viewport)

        assertEquals(8f, topLeft.x, .001f)
        assertEquals(8f, topLeft.y, .001f)
        assertEquals(692f, bottomRight.x, .001f)
        assertEquals(472f, bottomRight.y, .001f)
    }

    @Test fun boundsAccountForScaleAndRotation() {
        val card = card("transformed")
        val viewport = CardViewport(widthPx = 900f, heightPx = 600f, density = 1f)

        val scaled = CardLayoutEngine.constrainToViewport(card, CardTransform(-500f, 20f, 2f, 0f), viewport)
        val rotated = CardLayoutEngine.constrainToViewport(card, CardTransform(-500f, -300f, 1f, 90f), viewport)

        assertEquals(108f, scaled.x, .001f)
        assertEquals(-32f, rotated.x, .001f)
        assertEquals(48f, rotated.y, .001f)
    }

    @Test fun oversizedCardsStayCenteredSoTheyRemainOperable() {
        val oversized = card("oversized")
        val viewport = CardViewport(widthPx = 300f, heightPx = 160f, density = 1f)

        val result = CardLayoutEngine.constrainToViewport(
            oversized,
            CardTransform(-1000f, 800f, 2f, 0f),
            viewport,
        )

        assertEquals(50f, result.x, .001f)
        assertEquals(20f, result.y, .001f)
    }

    @Test fun dragDeltaCannotMoveCardOutsideViewport() {
        val card = card("drag")
        val viewport = CardViewport(widthPx = 900f, heightPx = 600f, density = 1f)

        val result = CardLayoutEngine.applyDeltaWithinViewport(
            card,
            CardTransform(100f, 100f, 1f, 0f),
            panX = 5000f,
            panY = -5000f,
            zoom = 1f,
            rotation = 0f,
            viewport = viewport,
        )

        assertEquals(692f, result.x, .001f)
        assertEquals(8f, result.y, .001f)
    }

    @Test fun layerOrderingProducesDeterministicFrontAndBack() {
        val cards = listOf(card("a", -2), card("b", 5), card("c", 1))
        assertEquals(6, CardLayoutEngine.nextLayer(cards, true))
        assertEquals(-3, CardLayoutEngine.nextLayer(cards, false))
    }

    @Test fun inertiaMovesInVelocityDirectionButCapsExtremeVelocity() {
        val base = CardTransform(100f, 100f, 1f, 0f)
        val regular = CardLayoutEngine.inertialProjection(base, 1000f, -500f)
        val extreme = CardLayoutEngine.inertialProjection(base, 100000f, 0f)
        assertTrue(regular.x > base.x)
        assertTrue(regular.y < base.y)
        assertTrue(extreme.x - base.x < 70f)
    }
}
