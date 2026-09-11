package com.spatialapps.inspirationwall.domain

import com.spatialapps.inspirationwall.data.CardEntity
import com.spatialapps.inspirationwall.data.CardTransform
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class CardViewport(
    val widthPx: Float,
    val heightPx: Float,
    val density: Float,
)

object CardLayoutEngine {
    fun isVisible(card: CardEntity, transform: CardTransform, viewportWidth: Float, viewportHeight: Float, margin: Float = 80f): Boolean =
        transform.x + card.width * transform.scale > -margin &&
            transform.y + card.height * transform.scale > -margin &&
            transform.x < viewportWidth + margin &&
            transform.y < viewportHeight + margin

    fun clamped(transform: CardTransform): CardTransform = transform.copy(scale = transform.scale.coerceIn(.55f, 2.2f))

    fun applyDelta(
        transform: CardTransform,
        panX: Float,
        panY: Float,
        zoom: Float,
        rotation: Float,
    ): CardTransform = clamped(
        transform.copy(
            x = transform.x + panX,
            y = transform.y + panY,
            scale = transform.scale * zoom,
            rotation = transform.rotation + rotation,
        ),
    )

    fun applyDeltaWithinViewport(
        card: CardEntity,
        transform: CardTransform,
        panX: Float,
        panY: Float,
        zoom: Float,
        rotation: Float,
        viewport: CardViewport,
    ): CardTransform = constrainToViewport(
        card,
        applyDelta(transform, panX, panY, zoom, rotation),
        viewport,
    )

    fun constrainToViewport(
        card: CardEntity,
        transform: CardTransform,
        viewport: CardViewport,
        insetDp: Float = 8f,
    ): CardTransform {
        val normalized = clamped(transform)
        if (viewport.widthPx <= 0f || viewport.heightPx <= 0f || viewport.density <= 0f) return normalized

        val cardWidthPx = card.width * viewport.density
        val cardHeightPx = card.height * viewport.density
        val radians = Math.toRadians((normalized.rotation % 360f).toDouble())
        val scaledWidth = cardWidthPx * normalized.scale
        val scaledHeight = cardHeightPx * normalized.scale
        val halfVisualWidth = (abs(scaledWidth * cos(radians)) + abs(scaledHeight * sin(radians))).toFloat() / 2f
        val halfVisualHeight = (abs(scaledWidth * sin(radians)) + abs(scaledHeight * cos(radians))).toFloat() / 2f
        val insetPx = insetDp * viewport.density

        val centerX = normalized.x + cardWidthPx / 2f
        val centerY = normalized.y + cardHeightPx / 2f
        val boundedCenterX = constrainCenter(centerX, halfVisualWidth, viewport.widthPx, insetPx)
        val boundedCenterY = constrainCenter(centerY, halfVisualHeight, viewport.heightPx, insetPx)
        return normalized.copy(
            x = boundedCenterX - cardWidthPx / 2f,
            y = boundedCenterY - cardHeightPx / 2f,
        )
    }

    private fun constrainCenter(center: Float, halfExtent: Float, viewportExtent: Float, inset: Float): Float {
        val minimum = inset + halfExtent
        val maximum = viewportExtent - inset - halfExtent
        return if (minimum <= maximum) center.coerceIn(minimum, maximum) else viewportExtent / 2f
    }

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
