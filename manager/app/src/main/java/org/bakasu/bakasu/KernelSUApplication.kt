package org.bakasu.bakasu

import android.app.Application
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        // 修复：合并为一个协程串行执行，防止底层 Shell 并发冲突导致 "The main shell was already created"
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                koin.get<InitializeApplicationUseCase>()()
            } catch (e: Exception) {
                // 忽略初始化异常
            }
            try {
                HuijuiDefaultsApplier.applyIfNeeded(this@KernelSUApplication)
            } catch (e: Exception) {
                // 忽略隐藏规则应用异常
            }
        }
    }
}
