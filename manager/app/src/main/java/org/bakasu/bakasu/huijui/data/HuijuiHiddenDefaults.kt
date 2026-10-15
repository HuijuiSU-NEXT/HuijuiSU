package org.bakasu.bakasu.huijui.data

/**
 * HuiJuiSU Next 内置隐藏默认值
 *
 * 用户不需要任何配置，装完就是最强隐藏状态。
 * 修改 DEFAULTS_VERSION 会触发下次启动重新应用。
 */
object HuijuiHiddenDefaults {

    /**
     * 内置规则版本号。每次修改 SUS_PATHS 后 +1，
     * App 启动时检测到版本变化会自动重新应用。
     */
    const val DEFAULTS_VERSION = 1

    /**
     * 默认隐藏的路径。
     *
     * 这些路径是常见检测器扫描的目标：
     * - /data/local/tmp：临时文件目录，检测 Root 时重点扫
     * - /data/adb/ksu：KernelSU 主目录
     * - /data/adb/ksud：ksud 可执行文件
     * - /data/adb/modules：模块目录
     * - /data/adb/modules_update：模块更新缓存
     */
    val SUS_PATHS = listOf(
        "/data/local/tmp",
        "/data/adb/ksu",
        "/data/adb/ksud",
        "/data/adb/modules",
        "/data/adb/modules_update",
    )

    /**
     * 是否启用 uname 伪装。
     *
     * 当内核 release 字符串包含 "kernelsu"、"ksu"、"HuijuiSU" 等关键字时，
     * 检测器会识别到 Root。启用伪装后，用户态看到的 release 会替换成原厂样式。
     */
    const val ENABLE_UNAME_SPOOF = true

    /**
     * uname 伪装的默认值。
     *
     * 只在 /proc/version 里包含敏感关键字时生效，
     * 否则用系统原值。
     */
    const val FAKE_KERNEL_RELEASE = "default"
}
