package org.bakasu.bakasu

import android.app.Application
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
                androidLogger()
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
        } catch (e: Throwable) {
            // 捕获异常，防止初始化失败导致崩溃
            android.util.Log.e("HuiJuiSU", "Application 初始化失败: ${e.message}", e)
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
}
