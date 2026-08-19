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
