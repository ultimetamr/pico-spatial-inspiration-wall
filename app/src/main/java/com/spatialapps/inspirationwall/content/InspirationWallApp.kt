package com.spatialapps.inspirationwall.content

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Subwindow
import com.pico.spatial.ui.design.windows.SubwindowPlacement
import com.pico.spatial.ui.platform.ability.UpperLimbRenderMode
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.openStage
import com.spatialapps.inspirationwall.data.AnchorState
import com.spatialapps.inspirationwall.data.CardType
import com.spatialapps.inspirationwall.data.WallStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InspirationWallApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WallStore(context) }
    val snapshot by store.snapshot.collectAsState()
    val previews by store.previewTransforms.collectAsState()
    val scope = rememberCoroutineScope()
    val guidePreferences = remember(context) {
        context.getSharedPreferences("inspiration-wall-onboarding", android.content.Context.MODE_PRIVATE)
    }

    var wallId by remember { mutableStateOf<String?>(null) }
    var groupId by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var editorId by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var undoId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("布局已同步到本地 Room 数据库") }
    var draftTitle by remember { mutableStateOf("") }
    var draftBody by remember { mutableStateOf("") }
    var pendingImageTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var guideOpen by remember { mutableStateOf(!guidePreferences.getBoolean(GUIDE_COMPLETED_KEY, false)) }
    var guideStep by remember { mutableStateOf(0) }
    val completeGuide = {
        guidePreferences.edit().putBoolean(GUIDE_COMPLETED_KEY, true).apply()
        guideOpen = false
        guideStep = 0
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = pendingImageTarget
        pendingImageTarget = null
        if (uri == null || target == null) {
            status = "已取消图片导入"
        } else {
            status = "正在导入图片…"
            store.createImageCard(target.first, target.second, uri, 360f, 220f) { result ->
                status = result.fold(
                    onSuccess = { "图片卡片已创建" },
                    onFailure = { "图片导入失败：${it.message ?: "无法读取文件"}" },
                )
            }
        }
    }

    LaunchedEffect(snapshot.walls) { if (wallId == null) wallId = snapshot.walls.firstOrNull()?.id }
    val wall = snapshot.walls.firstOrNull { it.id == wallId } ?: snapshot.walls.firstOrNull()
    val groups = snapshot.groups.filter { it.wallId == wall?.id }
    LaunchedEffect(wall?.id, groups) { if (groups.none { it.id == groupId }) groupId = groups.firstOrNull()?.id }
    val group = groups.firstOrNull { it.id == groupId }
    val wallCards = snapshot.cards.filter { it.wallId == wall?.id && it.type in SUPPORTED_CARD_TYPES }
    val cards = wallCards.filter { groupId == null || it.groupId == groupId }
    val selected = snapshot.cards.firstOrNull { it.id == selectedId }
    val editing = snapshot.cards.firstOrNull { it.id == editorId }

    // design-style: opaque-root (manifest materialbackground=0)
    Box(Modifier.fillMaxSize().background(PicoTheme.colorScheme.fillPrimary)) {
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                wall = wall,
                count = wallCards.size,
                onLibrary = {
                    libraryOpen = true
                    guideOpen = false
                    createOpen = false
                    editorId = null
                    deleteId = null
                },
                onAnchor = {
                    guideOpen = false
                    com.spatialapps.inspirationwall.platform.AnchorRuntime.activeWallId = wall?.id
                    scope.launch {
                        runCatching { context.openStage("WallAnchorStage", StageStyle.Mixed, Bundle(), UpperLimbRenderMode.Visible) }
                            .onFailure { status = "无法进入墙面扫描：${it.message}" }
                    }
                },
                onGuide = {
                    guideStep = 0
                    guideOpen = true
                    createOpen = false
                    libraryOpen = false
                    editorId = null
                    deleteId = null
                },
            )
            Row(Modifier.weight(1f)) {
                GroupRail(
                    groups = groups,
                    selectedId = groupId,
                    onSelect = { groupId = it; selectedId = null },
                    onAdd = { wall?.let { store.createGroup(it.id, "新主题 ${groups.size + 1}") } },
                )
                WallBoard(
                    modifier = Modifier.weight(1f).fillMaxHeight(), wall = wall, group = group,
                    cards = cards, previews = previews, selectedId = selectedId,
                    onSelect = { selectedId = it }, onTransformDelta = store::updateTransformDelta,
                    onCreate = {
                        guideOpen = false
                        createOpen = true
                        libraryOpen = false
                        editorId = null
                        deleteId = null
                    },
                )
            }
            WorkbenchStatus(status, cards.size)
        }

        AnimatedVisibility(selected != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).zIndex(80f)) {
            selected?.let { card ->
                CardToolbar(
                    onEdit = {
                        editorId = card.id
                        draftTitle = card.title
                        draftBody = card.content
                        createOpen = false
                        libraryOpen = false
                        deleteId = null
                    },
                    onGrow = { store.resize(card.id, 20f) }, onShrink = { store.resize(card.id, -20f) },
                    onFront = { store.moveLayer(card.id, true) }, onBack = { store.moveLayer(card.id, false) },
                    onDelete = {
                        deleteId = card.id
                        createOpen = false
                        libraryOpen = false
                        editorId = null
                    },
                )
            }
        }

        if (guideOpen || createOpen || libraryOpen || editing != null || deleteId != null) {
            Subwindow(placement = SubwindowPlacement.Right) {
                when {
                    guideOpen -> OnboardingGuide(
                        step = guideStep,
                        onBack = { guideStep = (guideStep - 1).coerceAtLeast(0) },
                        onNext = {
                            if (guideStep == GUIDE_LAST_STEP) completeGuide() else guideStep += 1
                        },
                        onDismiss = completeGuide,
                    )
                    deleteId != null -> DeleteConfirmation(onCancel = { deleteId = null }, onConfirm = {
                        deleteId?.let { store.delete(it); undoId = it }
                        deleteId = null; selectedId = null; status = "卡片已删除，可在 5 秒内撤销"
                    })
                    editing != null -> editing.let { card ->
                        CardEditor(card, draftTitle, draftBody, { draftTitle = it }, { draftBody = it }, onSave = {
                            store.updateContent(card.id, draftTitle, draftBody); editorId = null; status = "卡片已保存"
                        }, onDismiss = { editorId = null })
                    }
                    libraryOpen -> WallLibrary(snapshot.walls, wall?.id, onDismiss = { libraryOpen = false }, onSelect = {
                        wallId = it; libraryOpen = false; selectedId = null
                    }, onAdd = { store.createWall("灵感墙 ${snapshot.walls.size + 1}"); status = "已创建新的独立墙面" })
                    else -> CreatePalette(onDismiss = { createOpen = false }, onCreate = { type ->
                        if (wall != null && group != null) {
                            if (type == CardType.IMAGE) {
                                pendingImageTarget = wall.id to group.id
                                imagePicker.launch("image/*")
                                status = "请选择一张本地图片"
                            } else {
                                store.createCard(wall.id, group.id, type, 360f, 220f)
                                status = "${type.zhName()}卡片已创建"
                            }
                        }
                        createOpen = false
                    })
                }
            }
        }

        AnimatedVisibility(undoId != null, Modifier.align(Alignment.BottomEnd).padding(28.dp).zIndex(120f)) {
            PopupSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("卡片已删除", color = PicoTheme.colorScheme.labelPrimary)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    Button(
                        onClick = { undoId?.let(store::restore); undoId = null; status = "已恢复卡片" },
                        colors = inspirationButtonColors(),
                    ) { Text("撤销") }
                }
            }
        }
        LaunchedEffect(undoId) { if (undoId != null) { delay(5000); undoId = null } }
    }
}

internal fun CardType.zhName() = when (this) {
    CardType.TEXT -> "文字"; CardType.IMAGE -> "图片"; CardType.LINK -> "链接"
}

private val SUPPORTED_CARD_TYPES = CardType.entries.mapTo(mutableSetOf()) { it.name }
private const val GUIDE_COMPLETED_KEY = "guide-completed-v1"
private const val GUIDE_LAST_STEP = 2
