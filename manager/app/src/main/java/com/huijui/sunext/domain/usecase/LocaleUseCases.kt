package com.huijui.sunext.domain.usecase

import android.content.Context
import com.huijui.sunext.data.settings.LocaleRepository

class ApplyLanguageUseCase(private val repository: LocaleRepository) {
    operator fun invoke(ctx: Context): Context = repository.applyLanguage(ctx)
}

class IsSystemLanguageSettingsUseCase(private val repository: LocaleRepository) {
    operator fun invoke(): Boolean = repository.isSystemLanguageSettings()
}

class LaunchSystemLanguageSettingsUseCase(private val repository: LocaleRepository) {
    operator fun invoke(ctx: Context) = repository.launchSystemLanguageSettings(ctx)
}
