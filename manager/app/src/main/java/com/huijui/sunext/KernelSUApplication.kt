package com.huijui.sunext

import android.app.Application
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.huijui.sunext.di.appModules
import com.huijui.sunext.huijui.data.HuijuiDefaultsApplier
import com.huijui.sunext.domain.usecase.InitializeApplicationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.appContext.startKoin

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
        runBlocking(Dispatchers.IO) {
            koin.get<InitializeApplicationUseCase>()()
        }

        // 后台静默应用内置隐藏规则（不阻塞启动）
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            HuijuiDefaultsApplier.applyIfNeeded(this@KernelSUApplication)
        }
    }
}
