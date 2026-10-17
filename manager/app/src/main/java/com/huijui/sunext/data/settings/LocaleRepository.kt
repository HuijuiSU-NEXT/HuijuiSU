package com.huijui.sunext.data.settings

import android.content.Context
import android.os.Build
import com.huijui.sunext.data.settings.launchSystemLanguageSettings as launchSystemLanguageSettingsInternal

class LocaleRepository(
    private val localeHelper: LocaleHelper,
) {
    fun applyLanguage(ctx: Context): Context = localeHelper.applyLanguage(ctx)
    fun isSystemLanguageSettings(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    fun launchSystemLanguageSettings(ctx: Context) = launchSystemLanguageSettingsInternal(ctx)
}
