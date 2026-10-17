package com.huijui.sunext.domain.usecase

import com.huijui.sunext.data.update.ManagerUpdateRepository
import com.huijui.sunext.domain.model.ManagerUpdateChannel
import com.huijui.sunext.domain.model.ManagerUpdateInfo

class CheckManagerUpdateUseCase(
    private val repository: ManagerUpdateRepository,
) {
    suspend operator fun invoke(channel: ManagerUpdateChannel): ManagerUpdateInfo? = when (channel) {
        ManagerUpdateChannel.STABLE -> repository.checkStableUpdate()
        ManagerUpdateChannel.BETA -> repository.checkBetaUpdate()
    }
}
