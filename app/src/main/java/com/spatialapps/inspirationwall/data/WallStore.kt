package com.spatialapps.inspirationwall.data

import android.content.Context
import android.net.Uri
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.spatialapps.inspirationwall.domain.CardLayoutEngine
import com.spatialapps.inspirationwall.domain.CardViewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallStore(context: Context) {
    private val dao = WallDatabase.get(context).wallDao()
    private val assets = AssetStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transformJobs = ConcurrentHashMap<String, Job>()
    private val transformSamples = ConcurrentHashMap<String, Pair<CardTransform, Long>>()
    private val transformVelocities = ConcurrentHashMap<String, Pair<Float, Float>>()
    val previewTransforms = MutableStateFlow<Map<String, CardTransform>>(emptyMap())

    val snapshot: StateFlow<WallSnapshot> = combine(
        dao.observeWalls(),
        dao.observeGroups(),
        dao.observeCards(),
    ) { walls, groups, cards -> WallSnapshot(walls, groups, cards) }
        .stateIn(scope, SharingStarted.Eagerly, WallSnapshot())

    init {
        scope.launch {
            dao.deleteCardsByType(LEGACY_DOODLE_TYPE)
            dao.deleteCardsByContent(LEGACY_STRESS_CARD_CONTENT)
            assets.deleteLegacyDoodles()
            if (dao.wallCount() == 0) seedInitialContent()
            dao.purgeDeleted(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        }
    }

    fun createCard(wallId: String, groupId: String, type: CardType, x: Float, y: Float) {
        scope.launch {
            val id = UUID.randomUUID().toString()
            val current = snapshot.value.cards.filter { it.wallId == wallId }
            val top = (current.maxOfOrNull { it.zIndex } ?: 0) + 1
            val defaults = when (type) {
                CardType.TEXT -> Triple("一句灵感", "把模糊的念头，变成可以触摸的线索。", null)
                CardType.IMAGE -> Triple("色彩采样", "来自本地文件的图片卡片", assets.ensureDemoImage(id))
                CardType.LINK -> Triple("设计参考", "https://developer.picoxr.com/", null)
            }
            dao.upsertCard(
                CardEntity(
                    id = id,
                    wallId = wallId,
                    groupId = groupId,
                    type = type.name,
                    title = defaults.first,
                    content = defaults.second,
                    assetPath = defaults.third,
                    linkUrl = if (type == CardType.LINK) defaults.second else null,
                    x = x,
                    y = y,
                    width = if (type == CardType.IMAGE) 214f else 184f,
                    height = if (type == CardType.IMAGE) 156f else 126f,
                    rotation = listOf(-4f, -2f, 1.5f, 3f).random(),
                    zIndex = top,
                    paperStyle = top % 4,
                ),
            )
        }
    }

    fun createImageCard(
        wallId: String,
        groupId: String,
        source: Uri,
        x: Float,
        y: Float,
        onResult: (Result<String>) -> Unit,
    ) {
        scope.launch {
            val result = runCatching {
                val id = UUID.randomUUID().toString()
                val current = snapshot.value.cards.filter { it.wallId == wallId }
                val top = (current.maxOfOrNull { it.zIndex } ?: 0) + 1
                val assetPath = assets.importImage(id, source)
                dao.upsertCard(
                    CardEntity(
                        id = id,
                        wallId = wallId,
                        groupId = groupId,
                        type = CardType.IMAGE.name,
                        title = "本地图片",
                        content = "从设备文件中导入",
                        assetPath = assetPath,
                        x = x,
                        y = y,
                        width = 214f,
                        height = 156f,
                        rotation = listOf(-4f, -2f, 1.5f, 3f).random(),
                        zIndex = top,
                        paperStyle = top % 4,
                    ),
                )
                id
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun updateContent(cardId: String, title: String, content: String) {
        val card = snapshot.value.cards.firstOrNull { it.id == cardId } ?: return
        scope.launch { dao.upsertCard(card.copy(title = title, content = content, updatedAt = now())) }
    }

    fun updateTransform(cardId: String, transform: CardTransform) {
        updateTransform(cardId, transform, null)
    }

    private fun updateTransform(cardId: String, transform: CardTransform, viewport: CardViewport?) {
        val sourceCard = snapshot.value.cards.firstOrNull { it.id == cardId } ?: return
        val boundedTransform = viewport?.let { CardLayoutEngine.constrainToViewport(sourceCard, transform, it) }
            ?: CardLayoutEngine.clamped(transform)
        val now = System.currentTimeMillis()
        transformSamples.put(cardId, boundedTransform to now)?.let { (previous, previousAt) ->
            val seconds = ((now - previousAt).coerceAtLeast(1L)) / 1000f
            transformVelocities[cardId] = (boundedTransform.x - previous.x) / seconds to (boundedTransform.y - previous.y) / seconds
        }
        previewTransforms.value = previewTransforms.value + (cardId to boundedTransform)
        transformJobs.remove(cardId)?.cancel()
        transformJobs[cardId] = scope.launch {
            delay(140)
            val card = snapshot.value.cards.firstOrNull { it.id == cardId } ?: return@launch
            val velocity = transformVelocities.remove(cardId) ?: (0f to 0f)
            val projected = CardLayoutEngine.inertialProjection(boundedTransform, velocity.first, velocity.second)
            val persisted = viewport?.let { CardLayoutEngine.constrainToViewport(card, projected, it) } ?: projected
            dao.upsertCard(
                card.copy(
                    x = persisted.x,
                    y = persisted.y,
                    scale = persisted.scale,
                    rotation = persisted.rotation,
                    updatedAt = now(),
                ),
            )
            previewTransforms.value = previewTransforms.value - cardId
            transformSamples.remove(cardId)
            transformJobs.remove(cardId)
        }
    }

    fun updateTransformDelta(cardId: String, panX: Float, panY: Float, zoom: Float, rotation: Float, viewport: CardViewport) {
        val card = snapshot.value.cards.firstOrNull { it.id == cardId } ?: return
        val current = previewTransforms.value[cardId]
            ?: CardTransform(card.x, card.y, card.scale, card.rotation)
        updateTransform(
            cardId,
            CardLayoutEngine.applyDeltaWithinViewport(card, current, panX, panY, zoom, rotation, viewport),
            viewport,
        )
    }

    fun resize(cardId: String, delta: Float) {
        val card = snapshot.value.cards.firstOrNull { it.id == cardId } ?: return
        scope.launch {
            dao.upsertCard(
                card.copy(
                    width = (card.width + delta).coerceIn(132f, 420f),
                    height = (card.height + delta * 0.65f).coerceIn(92f, 320f),
                    updatedAt = now(),
                ),
            )
        }
    }

    fun moveLayer(cardId: String, toFront: Boolean) {
        val all = snapshot.value.cards
        val card = all.firstOrNull { it.id == cardId } ?: return
        val z = CardLayoutEngine.nextLayer(all, toFront)
        scope.launch { dao.upsertCard(card.copy(zIndex = z, updatedAt = now())) }
    }

    fun delete(cardId: String) = scope.launch { dao.softDeleteCard(cardId, now()) }

    fun restore(cardId: String) = scope.launch { dao.restoreCard(cardId, now()) }

    fun updateAnchor(wallId: String, uuid: String?, state: AnchorState) {
        val wall = snapshot.value.walls.firstOrNull { it.id == wallId } ?: return
        scope.launch {
            dao.upsertWall(wall.copy(anchorUuid = uuid, anchorState = state.name, updatedAt = now()))
        }
    }

    fun createWall(name: String, onCreated: (String) -> Unit = {}) {
        scope.launch {
            val wallId = UUID.randomUUID().toString()
            val groupId = UUID.randomUUID().toString()
            dao.upsertWall(WallEntity(wallId, name))
            dao.upsertGroup(GroupEntity(groupId, wallId, "收集箱", 0))
            onCreated(wallId)
        }
    }

    fun createGroup(wallId: String, name: String) {
        scope.launch {
            val order = snapshot.value.groups.count { it.wallId == wallId }
            dao.upsertGroup(GroupEntity(UUID.randomUUID().toString(), wallId, name, order))
        }
    }

    private suspend fun seedInitialContent() {
        val wallA = WallEntity("wall-studio", "工作室主墙")
        val wallB = WallEntity("wall-desk", "书桌侧墙")
        val groupIdeas = GroupEntity("group-ideas", wallA.id, "产品灵感", 0)
        val groupMood = GroupEntity("group-mood", wallA.id, "情绪板", 1)
        val groupDesk = GroupEntity("group-desk", wallB.id, "待读", 0)
        dao.upsertWall(wallA); dao.upsertWall(wallB)
        dao.upsertGroup(groupIdeas); dao.upsertGroup(groupMood); dao.upsertGroup(groupDesk)
        val cards = listOf(
            CardEntity("seed-text", wallA.id, groupIdeas.id, CardType.TEXT.name, "空间不是容器", "它是可被记忆、重新遇见的创作媒介。", x = 62f, y = 84f, width = 220f, height = 150f, rotation = -4f, zIndex = 1, paperStyle = 1),
            CardEntity("seed-image", wallA.id, groupMood.id, CardType.IMAGE.name, "柔和配色", "把绿色留给状态，把珊瑚色留给动作。", assetPath = assets.ensureDemoImage("seed"), x = 340f, y = 62f, width = 238f, height = 190f, rotation = 3f, zIndex = 3, paperStyle = 0),
            CardEntity("seed-link", wallA.id, groupIdeas.id, CardType.LINK.name, "PICO Spatial Design", "developer.picoxr.com/document/spatial-design/", linkUrl = "https://developer.picoxr.com/document/spatial-design/", x = 640f, y = 98f, width = 250f, height = 138f, rotation = -2f, zIndex = 2, paperStyle = 3),
            CardEntity("seed-quote", wallA.id, groupIdeas.id, CardType.TEXT.name, "今天的小目标", "让 50 张卡片仍然像一面墙，而不是一张表格。", x = 510f, y = 330f, width = 260f, height = 146f, rotation = -3f, zIndex = 4, paperStyle = 2),
            CardEntity("seed-desk", wallB.id, groupDesk.id, CardType.TEXT.name, "下次阅读", "空间锚点的重定位与恢复策略", x = 190f, y = 130f, width = 240f, height = 140f, rotation = 2f, zIndex = 1, paperStyle = 1),
        )
        dao.upsertCards(cards)
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val LEGACY_DOODLE_TYPE = "DOODLE"
        const val LEGACY_STRESS_CARD_CONTENT = "批量渲染与视锥剔除样例"
    }
}
