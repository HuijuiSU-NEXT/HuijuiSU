package com.huijui.sunext.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.huijui.sunext.domain.model.HomeDashboardState

class HomeStateRepository {
    private val mutableState = MutableStateFlow(HomeDashboardState())
    val state: StateFlow<HomeDashboardState> = mutableState.asStateFlow()

    fun update(transform: (HomeDashboardState) -> HomeDashboardState) {
        mutableState.update(transform)
    }
}
