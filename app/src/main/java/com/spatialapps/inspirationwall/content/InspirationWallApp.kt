package com.spatialapps.inspirationwall.content

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.ability.UpperLimbRenderMode
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.openStage
import com.spatialapps.inspirationwall.data.AnchorState
import com.spatialapps.inspirationwall.data.CardType
import com.spatialapps.inspirationwall.data.WallStore
import com.spatialapps.inspirationwall.platform.SpeechInput
import com.spatialapps.inspirationwall.platform.WallExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InspirationWallApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WallStore(context) }
    val snapshot by store.snapshot.collectAsState()
    val previews by store.previewTransforms.collectAsState()
    val scope = rememberCoroutineScope()
    val speech = remember { SpeechInput(context) }
    DisposableEffect(Unit) { onDispose { speech.release() } }

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

    LaunchedEffect(snapshot.walls) { if (wallId == null) wallId = snapshot.walls.firstOrNull()?.id }
    val wall = snapshot.walls.firstOrNull { it.id == wallId } ?: snapshot.walls.firstOrNull()
    val groups = snapshot.groups.filter { it.wallId == wall?.id }
    LaunchedEffect(wall?.id, groups) { if (groups.none { it.id == groupId }) groupId = groups.firstOrNull()?.id }
    val group = groups.firstOrNull { it.id == groupId }
    val wallCards = snapshot.cards.filter { it.wallId == wall?.id }
    val cards = wallCards.filter { groupId == null || it.groupId == groupId }
    val selected = snapshot.cards.firstOrNull { it.id == selectedId }
    val editing = snapshot.cards.firstOrNull { it.id == editorId }

    // design-style: opaque-root (manifest materialbackground=0)
    Box(Modifier.fillMaxSize().background(Color(0xFFF3EEE5))) { // design-style: fixed-figma-color opaque paper shell
        Column(Modifier.fillMaxSize()) {
            WorkbenchTopBar(
                wall = wall,
                count = wallCards.size,
                onLibrary = { libraryOpen = true },
                onAnchor = {
                    com.spatialapps.inspirationwall.platform.AnchorRuntime.activeWallId = wall?.id
                    scope.launch {
                        runCatching { context.openStage("WallAnchorStage", StageStyle.Mixed, Bundle(), UpperLimbRenderMode.Visible) }
                            .onFailure { status = "无法进入墙面扫描：${it.message}" }
                    }
                },
                onExport = {
                    if (wall != null) scope.launch {
                        status = "正在渲染完整墙面 PNG…"
                        val result = withContext(Dispatchers.IO) { WallExporter.export(context, wall.name, wallCards) }
                        status = result.fold({ "已导出到 Pictures/InspirationWall" }, { "导出失败：${it.message}" })
                    }
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
                    onSelect = { selectedId = it }, onTransform = store::updateTransform,
                    onCreate = { createOpen = true },
                )
            }
            WorkbenchStatus(status, cards.size) {
                if (wall != null && group != null) {
                    store.seedStressCards(wall.id, group.id)
                    status = "已准备 56 张卡片的批量渲染压力样例"
                }
            }
        }

        AnimatedVisibility(selected != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).zIndex(80f)) {
            selected?.let { card ->
                CardToolbar(
                    onEdit = { editorId = card.id; draftTitle = card.title; draftBody = card.content },
                    onGrow = { store.resize(card.id, 20f) }, onShrink = { store.resize(card.id, -20f) },
                    onFront = { store.moveLayer(card.id, true) }, onBack = { store.moveLayer(card.id, false) },
                    onDelete = { deleteId = card.id },
                )
            }
        }

        CreatePalette(createOpen, onDismiss = { createOpen = false }, onCreate = { type ->
            if (wall != null && group != null) {
                store.createCard(wall.id, group.id, type, 360f, 220f)
                status = "${type.zhName()}卡片已从手势位置生长"
            }
            createOpen = false
        }, onVoice = {
            status = "正在聆听；不会保存音频"
            speech.start(
                onPartial = { status = "识别中：$it" },
                onResult = { text ->
                    if (wall != null && group != null) {
                        store.createTextCardFromSpeech(wall.id, group.id, text, 380f, 230f)
                    }
                    status = "已创建语音文字卡片：$text"
                },
                onError = { status = it },
            )
        })

        WallLibrary(libraryOpen, snapshot.walls, wall?.id, onDismiss = { libraryOpen = false }, onSelect = {
            wallId = it; libraryOpen = false; selectedId = null
        }, onAdd = { store.createWall("灵感墙 ${snapshot.walls.size + 1}"); status = "已创建新的独立墙面" })

        editing?.let { card ->
            CardEditor(card, draftTitle, draftBody, { draftTitle = it }, { draftBody = it }, onVoice = {
                speech.start({ draftBody = it }, { draftBody = it }, { status = it })
            }, onSave = {
                store.updateContent(card.id, draftTitle, draftBody); editorId = null; status = "卡片已保存"
            }, onDismiss = { editorId = null })
        }

        if (deleteId != null) DeleteConfirmation(onCancel = { deleteId = null }, onConfirm = {
            deleteId?.let { store.delete(it); undoId = it }
            deleteId = null; selectedId = null; status = "卡片已删除，可在 5 秒内撤销"
        })

        AnimatedVisibility(undoId != null, Modifier.align(Alignment.BottomEnd).padding(28.dp).zIndex(120f)) {
            PopupSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("卡片已删除", color = PicoTheme.colorScheme.labelPrimary)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    Button(onClick = { undoId?.let(store::restore); undoId = null; status = "已恢复卡片" }) { Text("撤销") }
                }
            }
        }
        LaunchedEffect(undoId) { if (undoId != null) { delay(5000); undoId = null } }
    }
}

internal fun CardType.zhName() = when (this) {
    CardType.TEXT -> "文字"; CardType.IMAGE -> "图片"; CardType.LINK -> "链接"; CardType.DOODLE -> "涂鸦"
}
