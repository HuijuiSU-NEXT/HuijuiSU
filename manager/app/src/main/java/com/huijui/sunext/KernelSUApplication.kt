package com.huijui.sunext

import android.app.Application
import android.os.Build

/**
 * 精简后的 Application。
 *
 * 经典界面（ui / data / domain / di / profile / magica / huijui）已移出源码集，
 * 这里只保留 Nova 需要的最小初始化。Koin、Root Shell 预热等重初始化一并移除，
 * 冷启动更快，也不再依赖任何被移除的模块。
 */
class KernelSUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (processName.endsWith("MagicaService")) return
            if (processName.endsWith(":nova")) return
        }
    }
}
