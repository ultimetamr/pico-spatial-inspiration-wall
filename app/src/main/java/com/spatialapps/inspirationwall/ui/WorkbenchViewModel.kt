package com.spatialapps.inspirationwall.ui

import androidx.lifecycle.ViewModel
import com.spatialapps.inspirationwall.data.InspirationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WorkbenchViewModel(private val repository: InspirationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(WorkbenchUiState())
    val state: StateFlow<WorkbenchUiState> = mutableState

    fun onEvent(event: WorkbenchEvent) {
        mutableState.value = when (event) {
            is WorkbenchEvent.SelectWall -> mutableState.value.copy(selectedWallId = event.id, selectedCardId = null)
            is WorkbenchEvent.SelectGroup -> mutableState.value.copy(selectedGroupId = event.id, selectedCardId = null)
            is WorkbenchEvent.SelectCard -> mutableState.value.copy(selectedCardId = event.id)
        }
    }
}
