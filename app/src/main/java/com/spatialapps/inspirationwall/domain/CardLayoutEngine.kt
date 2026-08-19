package com.spatialapps.inspirationwall.domain

import com.spatialapps.inspirationwall.data.CardEntity
import com.spatialapps.inspirationwall.data.CardTransform

object CardLayoutEngine {
    fun isVisible(card: CardEntity, transform: CardTransform, viewportWidth: Float, viewportHeight: Float, margin: Float = 80f): Boolean =
        transform.x + card.width * transform.scale > -margin &&
            transform.y + card.height * transform.scale > -margin &&
            transform.x < viewportWidth + margin &&
            transform.y < viewportHeight + margin

    fun clamped(transform: CardTransform): CardTransform = transform.copy(scale = transform.scale.coerceIn(.55f, 2.2f))

    fun inertialProjection(transform: CardTransform, velocityX: Float, velocityY: Float): CardTransform {
        val horizonSeconds = .09f
        val friction = .34f
        return clamped(transform.copy(
            x = transform.x + velocityX.coerceIn(-2200f, 2200f) * horizonSeconds * friction,
            y = transform.y + velocityY.coerceIn(-2200f, 2200f) * horizonSeconds * friction,
        ))
    }

    fun nextLayer(cards: List<CardEntity>, toFront: Boolean): Int = if (toFront) {
        (cards.maxOfOrNull { it.zIndex } ?: 0) + 1
    } else {
        (cards.minOfOrNull { it.zIndex } ?: 0) - 1
    }
}
