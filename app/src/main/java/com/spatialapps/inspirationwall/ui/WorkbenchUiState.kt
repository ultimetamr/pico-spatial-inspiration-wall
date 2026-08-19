package com.spatialapps.inspirationwall.ui

import com.spatialapps.inspirationwall.data.WallSnapshot

data class WorkbenchUiState(
    val snapshot: WallSnapshot = WallSnapshot(),
    val selectedWallId: String? = null,
    val selectedGroupId: String? = null,
    val selectedCardId: String? = null,
)

sealed interface WorkbenchEvent {
    data class SelectWall(val id: String) : WorkbenchEvent
    data class SelectGroup(val id: String) : WorkbenchEvent
    data class SelectCard(val id: String?) : WorkbenchEvent
}
