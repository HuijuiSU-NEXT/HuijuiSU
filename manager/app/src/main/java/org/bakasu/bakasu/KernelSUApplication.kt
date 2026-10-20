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

        val koin = startKoin {
            androidLogger()
            androidContext(this@KernelSUApplication)
            modules(appModules)
        }.koin
        // 修复：绝对不能在 Application 中 runBlocking 阻塞主线程，改为纯异步协程
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                koin.get<InitializeApplicationUseCase>()()
            } catch (e: Exception) {
                // 忽略初始化异常，防止 App 启动直接闪退
            }
        }

        // 后台静默应用内置隐藏规则（不阻塞启动）
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            HuijuiDefaultsApplier.applyIfNeeded(this@KernelSUApplication)
        }
    }
}
