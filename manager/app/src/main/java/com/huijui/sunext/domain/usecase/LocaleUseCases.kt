package com.huijui.sunext.domain.usecase

import android.content.Context
import com.huijui.sunext.data.settings.LocaleRepository

class ApplyLanguageUseCase(private val repository: LocaleRepository) {
    operator fun invoke(appContext: Context): Context = repository.applyLanguage(context)
}

class IsSystemLanguageSettingsUseCase(private val repository: LocaleRepository) {
    operator fun invoke(): Boolean = repository.isSystemLanguageSettings()
}

class LaunchSystemLanguageSettingsUseCase(private val repository: LocaleRepository) {
    operator fun invoke(appContext: Context) = repository.launchSystemLanguageSettings(context)
}
