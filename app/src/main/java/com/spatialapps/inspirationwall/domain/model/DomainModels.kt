package com.spatialapps.inspirationwall.domain.model

import com.spatialapps.inspirationwall.data.AnchorState
import com.spatialapps.inspirationwall.data.CardType

data class InspirationCardModel(
    val id: String,
    val type: CardType,
    val title: String,
    val body: String,
    val transform: SpatialCardTransform,
    val zIndex: Int,
)

data class SpatialCardTransform(val x: Float, val y: Float, val scale: Float, val rotationDegrees: Float)

data class InspirationWallModel(val id: String, val name: String, val anchorState: AnchorState, val cards: List<InspirationCardModel>)
