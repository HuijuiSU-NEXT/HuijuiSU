package org.bakasu.bakasu

import android.app.Application
import org.bakasu.bakasu.BuildConfig
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bakasu.bakasu.di.appModules
import org.bakasu.bakasu.huijui.data.HuijuiDefaultsApplier
import org.bakasu.bakasu.domain.usecase.InitializeApplicationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KernelSUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (processName.endsWith("MagicaService")) {
                // avoid loading unnecessary thing when starting MagicaService
                return
            }
        }

        // 防御性初始化：防止 Activity 重建或进程快速重启时重复启动 Koin
        val koin = runCatching {
            startKoin {
                // release 下关掉 Koin 的逐条日志：日志本身在冷启动里能占几十毫秒
                androidLogger(
                    if (BuildConfig.DEBUG) org.koin.core.logger.Level.DEBUG
                    else org.koin.core.logger.Level.NONE,
                )
                androidContext(this@KernelSUApplication)
                modules(appModules)
            }.koin
        }.getOrElse { 
            // 如果已经启动过，则直接获取现有的 Koin 实例
            org.koin.core.context.GlobalContext.get() 
        }
        // 恢复核心：必须在主线程前完成 Shell 初始化，否则会导致 The main shell was already created
        try {
            runBlocking(Dispatchers.IO) {
                koin.get<InitializeApplicationUseCase>()()
            }
        } catch (e: Exception) {
            // 捕获异常，防止初始化失败导致崩溃
        }

        // 隐藏规则放在后台慢慢跑
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                HuijuiDefaultsApplier.applyIfNeeded(this@KernelSUApplication)
            } catch (e: Exception) {
                // 忽略隐藏规则应用异常
            }
        }
    }

    /**
     * 崩溃日志：写到 cache/crash/，用户可在「设置 → 关于」或反馈里带上。
     * 只记录，不吞异常——原行为保持不变。
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = java.io.File(cacheDir, "crash").apply { mkdirs() }
                val file = java.io.File(
                    dir,
                    "crash-${System.currentTimeMillis()}.txt",
                )
                file.writeText(
                    buildString {
                        appendLine("time=${java.util.Date()}")
                        appendLine("thread=${thread.name}")
                        appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                        appendLine("version=${packageManager.getPackageInfo(packageName, 0).versionName}")
                        appendLine()
                        val sw = java.io.StringWriter()
                        throwable.printStackTrace(java.io.PrintWriter(sw))
                        append(sw.toString())
                    },
                )
                // 只保留最近 10 份
                dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(10)?.forEach { it.delete() }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
