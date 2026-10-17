package com.huijui.sunext.domain.usecase

import java.io.File
import com.huijui.sunext.data.logging.BugreportRepository

class GenerateBugreportUseCase(
    private val repository: BugreportRepository,
) {
    operator fun invoke(): File = repository.create()
}
