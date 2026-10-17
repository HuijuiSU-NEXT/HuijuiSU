package com.huijui.sunext.data.settings

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale
import com.huijui.sunext.data.AppSettingsRepository

/**
 * Launch system app locale settings (Android 13+)
 */
fun launchSystemLanguageSettings(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to app language settings if system settings not available
        }
    }
}

/**
 * Apply saved language setting to ctx (for Android < 13)
 */
class LocaleHelper(
    private val settings: AppSettingsRepository,
) {
    fun applyLanguage(ctx: Context): Context {
        // On Android 13+, language is handled by system
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ctx
        }

        val localeTag = settings.getString("app_locale", "system") ?: "system"

        return if (localeTag == "system") {
            ctx
        } else {
            val locale = parseLocaleTag(localeTag)
            setLocale(ctx, locale)
        }
    }

/**
     * Set locale for ctx (Android < 13)
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun setLocale(ctx: Context, locale: Locale): Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        updateResources(ctx, locale)
    } else {
        updateResourcesLegacy(ctx, locale)
    }

    @SuppressLint("UseRequiresApi", "ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(ctx: Context, locale: Locale): Context {
        val configuration = Configuration()
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return ctx.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    @SuppressWarnings("deprecation")
    private fun updateResourcesLegacy(ctx: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val resources = ctx.resources
        val configuration = resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return ctx
    }

/**
     * Parse locale tag to Locale object
     */
    private fun parseLocaleTag(tag: String): Locale = try {
        if (tag.contains("_")) {
            val parts = tag.split("_")
            Locale.Builder()
                .setLanguage(parts[0])
                .setRegion(parts.getOrNull(1) ?: "")
                .build()
        } else {
            Locale.Builder()
                .setLanguage(tag)
                .build()
        }
    } catch (_: Exception) {
        Locale.getDefault()
    }

/**
     * Get current app locale
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getCurrentAppLocale(ctx: Context): Locale? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            val localeManager =
                ctx.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
            val locales = localeManager?.applicationLocales
            if (locales != null && !locales.isEmpty) {
                locales.get(0)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    } else {
        val localeTag = settings.getString("app_locale", "system") ?: "system"
        if (localeTag == "system") {
            null
        } else {
            parseLocaleTag(localeTag)
        }
    }
}
