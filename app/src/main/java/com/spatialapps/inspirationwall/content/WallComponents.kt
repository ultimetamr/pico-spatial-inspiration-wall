@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.spatialapps.inspirationwall.content

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextArea
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.spatialapps.inspirationwall.data.AnchorState
import com.spatialapps.inspirationwall.data.CardEntity
import com.spatialapps.inspirationwall.data.CardTransform
import com.spatialapps.inspirationwall.data.CardType
import com.spatialapps.inspirationwall.data.GroupEntity
import com.spatialapps.inspirationwall.data.WallEntity
import com.spatialapps.inspirationwall.data.WallStore
import com.spatialapps.inspirationwall.domain.CardLayoutEngine
import com.spatialapps.inspirationwall.platform.AnchorRuntime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun WorkbenchTopBar(wall: WallEntity?, count: Int, onLibrary: () -> Unit, onAnchor: () -> Unit, onExport: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("桌面空间灵感墙", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary)
            Text("${wall?.name ?: "正在载入"} · $count 张卡片", color = PicoTheme.colorScheme.labelSecondary, fontSize = 14.sp)
        }
        Button(onClick = onLibrary) { Text("多墙") }
        Spacer(Modifier.width(10.dp))
        Button(onClick = onAnchor) { Text(if (wall?.anchorState == AnchorState.BOUND.name) "◆ 已锚定" else "锚定墙面") }
        Spacer(Modifier.width(10.dp))
        Button(onClick = onExport) { Text("导出 PNG") }
    }
}

@Composable
internal fun GroupRail(groups: List<GroupEntity>, selectedId: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.width(208.dp).fillMaxHeight().padding(14.dp).clip(RoundedCornerShape(20.dp))
            .background(PicoTheme.colorScheme.fillSecondary).padding(14.dp),
    ) {
        Text("主题分组", fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { group -> SelectableChip(group.name, group.id == selectedId) { onSelect(group.id) } }
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("＋ 新分组") }
    }
}

@Composable
internal fun WallBoard(
    modifier: Modifier,
    wall: WallEntity?,
    group: GroupEntity?,
    cards: List<CardEntity>,
    previews: Map<String, CardTransform>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onTransform: (String, CardTransform) -> Unit,
    onCreate: () -> Unit,
) {
    BoxWithConstraints(
        modifier.padding(14.dp).clip(RoundedCornerShape(24.dp))
            // design-style: fixed-color(reason=paper-canvas medium)
            .background(Color(0xFFE9E1D2)).combinedClickable(onClick = { onSelect(null) }, onLongClick = onCreate), // design-style: fixed-figma-color paper-canvas medium
    ) {
        val density = LocalDensity.current
        val vw = with(density) { maxWidth.toPx() }
        val vh = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val line = Color(0x1F51483C) // design-style: fixed-figma-color paper-grid decoration
            var x = 0f
            while (x < size.width) { drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f); x += 44f }
            var y = 0f
            while (y < size.height) { drawLine(line, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f); y += 44f }
        }
        Column(Modifier.align(Alignment.TopStart).padding(20.dp).zIndex(70f)) {
            Text(wall?.name ?: "灵感墙", fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary)
            Text(group?.name ?: "全部", fontSize = 13.sp, color = PicoTheme.colorScheme.labelSecondary)
        }
        cards.asSequence().filter { card ->
            CardLayoutEngine.isVisible(
                card,
                previews[card.id] ?: CardTransform(card.x, card.y, card.scale, card.rotation),
                vw,
                vh,
            )
        }.sortedBy { it.zIndex }.forEach { card ->
            InspirationCard(
                card, previews[card.id] ?: CardTransform(card.x, card.y, card.scale, card.rotation), selectedId == card.id,
                onSelect = { onSelect(card.id) }, onTransform = { onTransform(card.id, it) },
            )
        }
        Button(onClick = onCreate, modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp).size(62.dp)) { Text("＋", fontSize = 28.sp) }
    }
}

@Composable
private fun InspirationCard(card: CardEntity, transform: CardTransform, selected: Boolean, onSelect: () -> Unit, onTransform: (CardTransform) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.offset { IntOffset(transform.x.roundToInt(), transform.y.roundToInt()) }.zIndex(card.zIndex.toFloat())
            .graphicsLayer {
                scaleX = transform.scale
                scaleY = transform.scale
                rotationZ = transform.rotation
            }
            .size(card.width.dp, card.height.dp).shadow(if (selected) 18.dp else 8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(PAPER_COLORS[card.paperStyle.mod(PAPER_COLORS.size)])
            .then(if (selected) Modifier.border(3.dp, PicoTheme.colorScheme.interaction, RoundedCornerShape(8.dp)) else Modifier)
            .spatialHoverEffect()
            .pointerInput(card.id, transform) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onTransform(CardTransform(transform.x + pan.x, transform.y + pan.y, (transform.scale * zoom).coerceIn(.55f, 2.2f), transform.rotation + rotation))
                }
            }
            .combinedClickable(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect() },
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect() },
            ).padding(if (card.type == CardType.IMAGE.name) 8.dp else 14.dp),
    ) {
        if (transform.scale < 0.9f) {
            Text(card.title, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            when (CardType.valueOf(card.type)) {
                CardType.TEXT -> TextCard(card)
                CardType.IMAGE -> ImageCard(card)
                CardType.LINK -> LinkCard(card)
                CardType.DOODLE -> DoodleCard(card)
            }
        }
        if (selected) Text("拖拽 · 双指缩放/旋转", Modifier.align(Alignment.BottomCenter), fontSize = 10.sp, color = PicoTheme.colorScheme.interaction)
    }
}

@Composable private fun TextCard(card: CardEntity) = Column {
    Text(card.title, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(8.dp)); Text(card.content, color = PicoTheme.colorScheme.labelSecondary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
}

@Composable private fun ImageCard(card: CardEntity) {
    val bitmap = remember(card.assetPath) { card.assetPath?.let(BitmapFactory::decodeFile)?.asImageBitmap() }
    Column { if (bitmap != null) Image(bitmap, card.title, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop); Text(card.title, Modifier.padding(top = 5.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary) }
}

@Composable private fun LinkCard(card: CardEntity) = Column {
    Text("↗ 网页灵感", color = PicoTheme.colorScheme.interaction, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp))
    Text(card.title, color = PicoTheme.colorScheme.labelPrimary, fontWeight = FontWeight.Bold); Text(card.content, color = PicoTheme.colorScheme.labelSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable private fun DoodleCard(card: CardEntity) = Box(Modifier.fillMaxSize()) {
    Canvas(Modifier.fillMaxSize()) {
        val path = Path().apply { moveTo(size.width * .05f, size.height * .78f); cubicTo(size.width * .25f, size.height * .14f, size.width * .48f, size.height * .72f, size.width * .9f, size.height * .24f) }
        drawPath(path, Color(0xFFE97762), style = androidx.compose.ui.graphics.drawscope.Stroke(7f)); drawCircle(Color(0xFF43809B), 12f, androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .58f)) // design-style: fixed-figma-color doodle ink palette
    }
    Text(card.title, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary)
}

@Composable
internal fun CardToolbar(onEdit: () -> Unit, onGrow: () -> Unit, onShrink: () -> Unit, onFront: () -> Unit, onBack: () -> Unit, onDelete: () -> Unit) {
    PopupSurface { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("编辑" to onEdit, "放大" to onGrow, "缩小" to onShrink, "置顶" to onFront, "置底" to onBack, "删除" to onDelete).forEach { (label, action) -> Button(onClick = action) { Text(label) } }
    } }
}

@Composable
internal fun CreatePalette(visible: Boolean, onDismiss: () -> Unit, onCreate: (CardType) -> Unit, onVoice: () -> Unit) {
    ModalScrim(visible, onDismiss) { PopupSurface(Modifier.width(560.dp)) { Column {
        Text("贴一张新灵感", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary)
        Text("选择类型；创建后可自由拖拽、缩放和旋转", color = PicoTheme.colorScheme.labelSecondary); Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { CardType.entries.forEach { type -> Button(onClick = { onCreate(type) }, modifier = Modifier.weight(1f)) { Text(type.zhName()) } } }
        Spacer(Modifier.height(12.dp)); Row { Button(onClick = onVoice, modifier = Modifier.weight(1f)) { Text("🎙 语音文字卡片") }; Spacer(Modifier.width(10.dp)); Button(onClick = onDismiss) { Text("取消") } }
    } } }
}

@Composable
internal fun WallLibrary(visible: Boolean, walls: List<WallEntity>, selectedId: String?, onDismiss: () -> Unit, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    ModalScrim(visible, onDismiss) { PopupSurface(Modifier.width(520.dp)) { Column {
        Text("多墙管理", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary); Spacer(Modifier.height(14.dp))
        walls.forEach { wall -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(wall.name, fontWeight = FontWeight.Bold, color = PicoTheme.colorScheme.labelPrimary); Text(if (wall.anchorState == AnchorState.BOUND.name) "◆ 已定位" else "○ 未锚定", fontSize = 12.sp, color = PicoTheme.colorScheme.labelSecondary) }
            Button(onClick = { onSelect(wall.id) }) { Text(if (wall.id == selectedId) "当前" else "查看") }
        } }
        Spacer(Modifier.height(10.dp)); Row { Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("＋ 新建灵感墙") }; Spacer(Modifier.width(10.dp)); Button(onClick = onDismiss) { Text("完成") } }
    } } }
}

@Composable
internal fun CardEditor(card: CardEntity, title: String, body: String, onTitle: (String) -> Unit, onBody: (String) -> Unit, onVoice: () -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    ModalScrim(true, onDismiss) { PopupSurface(Modifier.width(620.dp)) { Column {
        Text("编辑${CardType.valueOf(card.type).zhName()}卡片", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary); Spacer(Modifier.height(12.dp))
        TextArea(title, onTitle, Modifier.fillMaxWidth().height(70.dp), placeholder = { Text("标题") }); Spacer(Modifier.height(10.dp))
        TextArea(body, onBody, Modifier.fillMaxWidth().height(170.dp), placeholder = { Text("灵感内容") }); Spacer(Modifier.height(12.dp))
        Row { Button(onClick = onVoice) { Text("🎙 语音补充") }; Spacer(Modifier.weight(1f)); Button(onClick = onDismiss) { Text("取消") }; Spacer(Modifier.width(8.dp)); Button(onClick = onSave) { Text("保存") } }
    } } }
}

@Composable internal fun DeleteConfirmation(onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalScrim(true, onCancel) { PopupSurface(Modifier.width(430.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("删除这张卡片？", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary); Text("删除后可在 5 秒内撤销。", color = PicoTheme.colorScheme.labelSecondary); Spacer(Modifier.height(18.dp))
        Row { Button(onClick = onCancel) { Text("保留") }; Spacer(Modifier.width(10.dp)); Button(onClick = onConfirm) { Text("确认删除") } }
    } } }
}

@Composable private fun ModalScrim(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = .98f), exit = fadeOut() + scaleOut(targetScale = .98f), modifier = Modifier.fillMaxSize().zIndex(100f)) {
        Box(Modifier.fillMaxSize().background(PicoTheme.colorScheme.fillPrimary.copy(alpha = .82f)).combinedClickable(onClick = onDismiss), contentAlignment = Alignment.Center) { Box(Modifier.combinedClickable(onClick = {})) { content() } }
    }
}

@Composable internal fun PopupSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.shadow(20.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(PicoTheme.colorScheme.fillSecondary).padding(20.dp)) { content() }
}

@Composable private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) PicoTheme.colorScheme.interaction else PicoTheme.colorScheme.fillPrimary)
        .combinedClickable(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }).padding(horizontal = 13.dp, vertical = 11.dp)) {
        Text(label, color = if (selected) PicoTheme.colorScheme.labelPrimaryLight else PicoTheme.colorScheme.labelPrimary, maxLines = 1)
    }
}

@Composable internal fun WorkbenchStatus(status: String, cardCount: Int, onStress: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(status, Modifier.weight(1f), fontSize = 12.sp, color = PicoTheme.colorScheme.labelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("可见 $cardCount · LOD/视锥剔除", fontSize = 12.sp, color = PicoTheme.colorScheme.labelSecondary); Spacer(Modifier.width(10.dp))
        Box(Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = onStress).padding(6.dp)) { Text("50+ 测试", fontSize = 12.sp, color = PicoTheme.colorScheme.interaction) }
    }
}

@Composable
fun AnchorStageScreen() {
    val context = LocalContext.current
    val store = remember { WallStore(context) }
    val state by AnchorRuntime.state.collectAsState()
    val navigator = LocalSpatialNavigator.current
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(PicoTheme.colorScheme.fillPrimary), contentAlignment = Alignment.Center) {
        PopupSurface(Modifier.width(680.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("墙面锚定阶段", style = PicoTheme.typography.titleLarge, color = PicoTheme.colorScheme.labelPrimary)
            Text("Full Space · Plane Detection · Persistent World Anchor", color = PicoTheme.colorScheme.labelSecondary)
            Spacer(Modifier.height(24.dp)); Text(state.message, color = if (state.state == AnchorState.ERROR) PicoTheme.colorScheme.error else PicoTheme.colorScheme.labelPrimary)
            Text("检测到 ${state.planeCount} 个竖直平面", color = PicoTheme.colorScheme.labelSecondary); Spacer(Modifier.height(22.dp))
            Row { Button(onClick = AnchorRuntime::scan) { Text("扫描墙面") }; Spacer(Modifier.width(10.dp)); Button(onClick = {
                AnchorRuntime.bind("InspirationWall") { uuid -> AnchorRuntime.activeWallId?.let { store.updateAnchor(it, uuid, AnchorState.BOUND) } }
            }, enabled = state.selectedPlane != null) { Text("确认并永久锚定") } }
            Spacer(Modifier.height(12.dp)); Button(onClick = { AnchorRuntime.stop(); scope.launch { navigator.closeStage() } }) { Text("返回共享空间工作台") }
            Text("模拟器不提供真实墙面/锚点数据；此阶段需在 PICO 头显完成设备验收。", Modifier.padding(top = 20.dp), fontSize = 12.sp, color = PicoTheme.colorScheme.labelSecondary)
        } }
    }
}

// design-style: fixed-color(reason=paper-media palette defined by product requirements)
private val PAPER_COLORS = listOf(Color(0xFFFBF8EE), Color(0xFFF9EAA3), Color(0xFFF7D8DD), Color(0xFFD3E5EE)) // design-style: fixed-figma-color paper-media palette
