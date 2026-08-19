package com.spatialapps.inspirationwall.data

import kotlinx.coroutines.flow.StateFlow

interface InspirationRepository {
    val snapshot: StateFlow<WallSnapshot>
    val previewTransforms: StateFlow<Map<String, CardTransform>>
    fun createCard(wallId: String, groupId: String, type: CardType, x: Float, y: Float)
    fun updateTransform(cardId: String, transform: CardTransform)
    fun updateContent(cardId: String, title: String, content: String)
    fun delete(cardId: String)
    fun restore(cardId: String)
}

class RoomInspirationRepository(private val store: WallStore) : InspirationRepository {
    override val snapshot = store.snapshot
    override val previewTransforms = store.previewTransforms
    override fun createCard(wallId: String, groupId: String, type: CardType, x: Float, y: Float) = store.createCard(wallId, groupId, type, x, y)
    override fun updateTransform(cardId: String, transform: CardTransform) = store.updateTransform(cardId, transform)
    override fun updateContent(cardId: String, title: String, content: String) = store.updateContent(cardId, title, content)
    override fun delete(cardId: String) { store.delete(cardId) }
    override fun restore(cardId: String) { store.restore(cardId) }
}
