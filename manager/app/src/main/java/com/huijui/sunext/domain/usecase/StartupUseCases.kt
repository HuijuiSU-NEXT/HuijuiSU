package com.huijui.sunext.domain.usecase

import com.huijui.sunext.data.startup.StartupRepository

class ObserveStartupStateUseCase(
    private val repository: StartupRepository,
) {
    operator fun invoke() = repository.state
}
