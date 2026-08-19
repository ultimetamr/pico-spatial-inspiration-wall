package com.spatialapps.inspirationwall.platform

import com.pico.spatial.sense.plane.PlaneAnchor
import com.pico.spatial.sense.plane.PlaneOrientation
import com.pico.spatial.sense.plane.PlaneTrackingManager
import com.pico.spatial.sense.world.WorldTrackingManager
import com.pico.spatial.sense.world.WorldTrackingResult
import com.spatialapps.inspirationwall.data.AnchorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AnchorUiState(
    val state: AnchorState = AnchorState.NOT_BOUND,
    val message: String = "尚未绑定物理墙面",
    val planeCount: Int = 0,
    val selectedPlane: PlaneAnchor? = null,
    val anchorUuid: String? = null,
)

object AnchorRuntime {
    var activeWallId: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(AnchorUiState())
    val state: StateFlow<AnchorUiState> = mutableState

    fun scan() {
        mutableState.value = AnchorUiState(AnchorState.SCANNING, "正在检测竖直墙面…")
        PlaneTrackingManager.start()
        scope.launch {
            runCatching { PlaneTrackingManager.loadAllAnchors() }
                .onSuccess { anchors ->
                    val vertical = anchors.filter { it.planeOrientation == PlaneOrientation.VERTICAL }
                    val best = vertical.maxByOrNull { it.boundingBoxSize.x * it.boundingBoxSize.y }
                    mutableState.value = if (best == null) {
                        AnchorUiState(AnchorState.ERROR, "未发现可用墙面，请面向空白墙缓慢环视", anchors.size)
                    } else {
                        AnchorUiState(AnchorState.SCANNING, "已发现 ${vertical.size} 面墙，捏合确认当前墙面", vertical.size, best)
                    }
                }
                .onFailure {
                    mutableState.value = AnchorUiState(AnchorState.UNAVAILABLE, "模拟器不提供真实平面数据；请在 PICO 设备验证")
                }
        }
    }

    fun bind(name: String, onBound: (String) -> Unit) {
        val plane = mutableState.value.selectedPlane ?: return
        scope.launch {
            val result = WorldTrackingManager.createAnchor(
                plane.transform.position,
                plane.transform.rotation,
                name,
            )
            when (result) {
                is WorldTrackingResult.Success -> {
                    val uuid = result.data?.anchorUUID?.toString()
                        ?: return@launch
                    mutableState.value = mutableState.value.copy(
                        state = AnchorState.BOUND,
                        message = "墙面锚点已持久保存",
                        anchorUuid = uuid,
                    )
                    onBound(uuid)
                }
                is WorldTrackingResult.Error -> mutableState.value = mutableState.value.copy(
                    state = AnchorState.ERROR,
                    message = "锚点保存失败：${result.errorMessage}",
                )
            }
        }
    }

    fun stop() = PlaneTrackingManager.stop()
}
