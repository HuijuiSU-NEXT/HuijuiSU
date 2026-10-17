package com.huijui.sunext.data.settings

import android.content.Context
import android.os.Build
import com.huijui.sunext.data.settings.launchSystemLanguageSettings as launchSystemLanguageSettingsInternal

class LocaleRepository(
    private val localeHelper: LocaleHelper,
) {
    fun applyLanguage(appContext: Context): Context = localeHelper.applyLanguage(context)
    fun isSystemLanguageSettings(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    fun launchSystemLanguageSettings(appContext: Context) = launchSystemLanguageSettingsInternal(context)
}
