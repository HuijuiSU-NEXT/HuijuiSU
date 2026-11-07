package com.huijui.sunext.nova

import android.content.Context
import com.huijui.nova.design.HuiAccent
import com.huijui.nova.design.HuiMaterial
import com.huijui.nova.design.HuiMotionLevel

/**
 * Nova 外观设置的持久化。
 *
 * 之前这些状态只存在 `rememberSaveable` 里，进程一死就回到默认；现在落到
 * SharedPreferences，重启后仍是上次那套配色/材质/节奏。
 */
object NovaSettingsStore {

    private const val PREFS = "nova_settings"
    private const val KEY_ACCENT = "accent"
    private const val KEY_MATERIAL = "material"
    private const val KEY_MOTION = "motion"
    private const val KEY_DARK = "dark"
    private const val KEY_MUTED = "muted"
    private const val KEY_UI_MODE = "ui_mode"
    private const val KEY_GLASS = "glass"
    private const val KEY_THEME_MODE = "theme_mode"

    data class Ui(
        val accent: HuiAccent,
        val material: HuiMaterial,
        val motion: HuiMotionLevel,
        val dark: Boolean,
        val muted: Boolean = false,
        val uiMode: com.huijui.nova.design.NovaUiMode = com.huijui.nova.design.NovaUiMode.Wallpaper,
        val glass: com.huijui.nova.design.NovaGlass = com.huijui.nova.design.NovaGlass.SoftGlow,
        val themeMode: com.huijui.nova.design.NovaThemeMode = com.huijui.nova.design.NovaThemeMode.System,
    )

    fun load(context: Context): Ui {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Ui(
            accent = HuiAccent.entries.getOrElse(prefs.getInt(KEY_ACCENT, HuiAccent.Miku.ordinal)) {
                HuiAccent.Miku
            },
            material = HuiMaterial.entries.getOrElse(
                prefs.getInt(KEY_MATERIAL, HuiMaterial.GlossyGlass.ordinal),
            ) { HuiMaterial.GlossyGlass },
            motion = HuiMotionLevel.entries.getOrElse(
                prefs.getInt(KEY_MOTION, HuiMotionLevel.Flow.ordinal),
            ) { HuiMotionLevel.Flow },
            dark = prefs.getBoolean(KEY_DARK, true),
            muted = prefs.getBoolean(KEY_MUTED, false),
            uiMode = com.huijui.nova.design.NovaUiMode.entries.getOrElse(
                prefs.getInt(KEY_UI_MODE, com.huijui.nova.design.NovaUiMode.Wallpaper.ordinal),
            ) { com.huijui.nova.design.NovaUiMode.Wallpaper },
            glass = com.huijui.nova.design.NovaGlass.entries.getOrElse(
                prefs.getInt(KEY_GLASS, com.huijui.nova.design.NovaGlass.SoftGlow.ordinal),
            ) { com.huijui.nova.design.NovaGlass.SoftGlow },
            themeMode = com.huijui.nova.design.NovaThemeMode.entries.getOrElse(
                prefs.getInt(KEY_THEME_MODE, com.huijui.nova.design.NovaThemeMode.System.ordinal),
            ) { com.huijui.nova.design.NovaThemeMode.System },
        )
    }

    fun save(context: Context, ui: Ui) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ACCENT, ui.accent.ordinal)
            .putInt(KEY_MATERIAL, ui.material.ordinal)
            .putInt(KEY_MOTION, ui.motion.ordinal)
            .putBoolean(KEY_DARK, ui.dark)
            .putBoolean(KEY_MUTED, ui.muted)
            .putInt(KEY_UI_MODE, ui.uiMode.ordinal)
            .putInt(KEY_GLASS, ui.glass.ordinal)
            .putInt(KEY_THEME_MODE, ui.themeMode.ordinal)
            .apply()
    }
}
