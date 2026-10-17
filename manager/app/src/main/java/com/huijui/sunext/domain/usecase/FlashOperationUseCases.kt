package com.huijui.sunext.domain.usecase

import com.huijui.sunext.data.flash.FlashRepository
import com.huijui.sunext.domain.model.FlashOperation

class ExecuteFlashOperationUseCase(private val repository: FlashRepository) {
    operator fun invoke(operation: FlashOperation) = repository.execute(operation)
}
