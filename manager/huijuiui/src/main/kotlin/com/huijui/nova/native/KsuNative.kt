package com.huijui.nova.native

/**
 * 内核 app_profile 通道。
 *
 * `libksuioctl.so` 由 app/src/main/cpp/ksu_ioctl.c 编译而来（三 ABI），
 * 通过 `syscall(__NR_reboot, MAGIC1, MAGIC2, 0, &fd)` 拿驱动 fd，
 * 再 `ioctl(fd, KSU_IOCTL_SET_APP_PROFILE, &profile)` 授权/撤销。
 *
 * 返回值：0 成功；-100 拿不到驱动 fd（没有 KernelSU / 非 LKM）；
 * 其余为 -errno。
 */
object KsuNative {

    private var loaded = false

    init {
        loaded = runCatching { System.loadLibrary("ksuioctl") }.isSuccess
    }

    val available: Boolean get() = loaded

    @JvmStatic
    external fun setAppProfile(uid: Int, pkg: String, allow: Boolean): Int

    @JvmStatic
    external fun driverAlive(): Int

    /** 安全封装：native 没加载时返回 -101。 */
    fun grant(uid: Int, pkg: String, allow: Boolean = true): Int =
        if (!loaded) -101 else runCatching { setAppProfile(uid, pkg, allow) }.getOrDefault(-102)
}
