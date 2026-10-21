package org.bakasu.bakasu.huijui.data

/**
 * HuiJuiSU NEXT 内置隐藏默认值
 *
 * 用户不需要任何配置，装完就是最强隐藏状态。
 * 修改 DEFAULTS_VERSION 会触发下次启动重新应用。
 */
object HuijuiHiddenDefaults {

    /**
     * 内置规则版本号。每次修改规则后 +1，
     * App 启动时检测到版本变化会自动重新应用。
     *
     * v1: sus_path 基础路径
     * v2: 扩充 sus_path + 开启 sus_mount 挂载隐藏 + avc 日志伪装 + sus_kstat
     */
    const val DEFAULTS_VERSION = 2

    /**
     * 默认隐藏的路径（sus_path）。
     *
     * 这些路径是常见检测器扫描的目标：
     * - /data/local/tmp：临时文件目录，检测 Root 时重点扫
     * - /data/adb/ksu：KernelSU 主目录
     * - /data/adb/ksud：ksud 可执行文件
     * - /data/adb/modules：模块目录
     * - /data/adb/modules_update：模块更新缓存
     * - /data/adb/magisk：Magisk 兼容目录（部分模块会创建）
     * - /sbin/.magisk：老版本 Magisk 的挂载点
     * - /debug_ramdisk：调试 ramdisk
     */
    val SUS_PATHS = listOf(
        "/data/local/tmp",
        "/data/adb/ksu",
        "/data/adb/ksud",
        "/data/adb/modules",
        "/data/adb/modules_update",
        "/data/adb/magisk",
        "/sbin/.magisk",
        "/debug_ramdisk")

    /**
     * 默认隐藏 kstat 的路径（sus_kstat）。
     *
     * 这些路径被模块挂载后会残留 mtime / inode 等统计信息，
     * 某些检测器会通过 stat 调用来判断是否被挂载覆盖。
     */
    val SUS_KSTATS = listOf(
        "/system",
        "/vendor")

    /**
     * 是否开启挂载点隐藏（hide_sus_mnts_for_non_su_procs）。
     *
     * 开启后，非 su 进程看不到 sus 挂载点，能对抗大部分基于
     * 挂载表扫描的检测。
     */
    const val ENABLE_SUS_MOUNT = true

    /**
     * 是否开启 AVC 日志伪装（avc_log_spoofing）。
     *
     * 隐藏 SELinux 拒绝日志里的敏感路径，避免被检测器通过
     * dmesg 读取 avc denied 日志反推 Root。
     */
    const val ENABLE_AVC_LOG_SPOOFING = true

    /**
     * uname 伪装的默认值。
     */
    const val FAKE_KERNEL_RELEASE = "default"
}
