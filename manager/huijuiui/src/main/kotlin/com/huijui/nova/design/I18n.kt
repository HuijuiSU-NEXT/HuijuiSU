package com.huijui.nova.design

import com.huijui.nova.design.S

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 轻量多语言。
 *
 * 设计取舍：文案以中文为 key，英文查表；查不到就回退中文，
 * 这样漏翻的地方不会变成空白或乱码，只是保持中文。
 */
enum class Lang(val label: String) { Zh("中文"), En("English") }

object I18n {
    var lang: Lang by mutableStateOf(Lang.Zh)

    private val en: Map<String, String> = mapOf(
        // 导航与页面
        "首页" to "Home", "模块" to "Modules", "超级用户" to "Superuser", "设置" to "Settings",
        "工具" to "Tools", "实验" to "Lab", "全部工具" to "All tools",
        "刷写中心" to "Flash", "刷写" to "Flash", "一键隐藏" to "Hide", "终端" to "Terminal",
        "春秋检测" to "Detection", "检测" to "Detect", "补丁中心" to "Patches",
        "一键体检" to "Checkup", "内核设置" to "Kernel", "开源许可" to "Licenses",
        "在线模块" to "Online modules", "在线" to "Online", "4.x 兼容模式" to "4.x mode",
        "SUSFS" to "SUSFS", "隐藏套件" to "Hide bundle",
        // 状态与通用
        "Root 已激活" to "Root active", "刷新" to "Refresh", "重测" to "Re-run",
        "检查更新" to "Check update", "网络不可用" to "Network unavailable",
        "获取失败" to "Failed", "读取中…" to "Loading…", "读取中" to "Loading",
        "检查中…" to "Checking…", "检测中…" to "Detecting…", "执行中…" to "Running…",
        "正在体检" to "Checking", "拉取索引中…" to "Fetching index…",
        "不可用" to "Unavailable", "不支持" to "Unsupported", "未知" to "Unknown",
        "正常" to "Normal", "安全" to "Safe", "谨慎" to "Caution", "危险" to "Danger",
        "已开启" to "On", "已关闭" to "Off", "启用" to "Enable", "停用" to "Disable",
        "已启用" to "Enabled", "已停用" to "Disabled", "使用中" to "In use",
        "允许" to "Allow", "拒绝" to "Deny", "取消" to "Cancel", "确认撤销" to "Revoke",
        "查看" to "View", "进入" to "Open", "进入 ▸" to "Open ▸", "关闭 Nova" to "Close Nova",
        "回到经典界面" to "Back to classic", "应用" to "Apply", "说明" to "Info",
        "复制" to "Copy", "已复制" to "Copied", "删除" to "Delete", "移除" to "Remove",
        "添加" to "Add", "加入" to "Join", "打开" to "Open", "选择" to "Choose",
        "安装" to "Install", "安装中…" to "Installing…", "已下载，开始安装" to "Downloaded, installing",
        "安装结束，重启后生效" to "Done, reboot to apply", "下载失败" to "Download failed",
        "重装" to "Reinstall", "启用后才会应用下面的隐藏规则" to "Rules apply only when enabled",
        "重启" to "Reboot", "重启设备" to "Reboot device", "关机" to "Power off",
        "重启到 Recovery" to "Reboot to Recovery", "重启到 Bootloader" to "Reboot to Bootloader",
        "清屏" to "Clear", "清除" to "Clear", "运行" to "Run", "等待" to "Waiting",
        "选择应用授予 Root" to "Pick an app to grant root", "搜索应用 / 包名" to "Search app / package",
        "搜索模块 / 作者" to "Search module / author", "授权" to "Grant",
        "没有授权记录" to "No granted apps", "读不到授权表（需要 Root）" to "Cannot read allowlist (needs root)",
        "没有匹配的模块" to "No matching modules", "导出到 /sdcard" to "Export to /sdcard",
        "恢复默认" to "Restore defaults", "配置备份" to "Config backup",
        "隐藏路径" to "Hidden paths", "还没有隐藏路径" to "No hidden paths yet",
        "SUSFS 总开关" to "SUSFS master switch", "日志" to "Logging", "AVC 日志伪造" to "AVC log spoofing",
        "隐藏 sus 挂载" to "Hide sus mounts", "记录 SUSFS 操作" to "Record SUSFS actions",
        "记录 su 调用日志" to "Log su calls",
        // 首页
        "内核" to "Kernel", "内存" to "RAM", "核心" to "Cores", "电池温度" to "Battery",
        "已装模块" to "Modules", "+1 待更新" to "+1 update", "快捷操作" to "Quick actions",
        "工作模式" to "Mode", "未工作" to "Not working", "非 GKI" to "Non-GKI",
        "冲突 · 隐藏 · 备份" to "Conflicts · Hide · Backup",
        "检测 · 终端 · 重启" to "Detect · Terminal · Reboot",
        "模块 · 修补 · 刷分区" to "Modules · Patch · Flash",
        "Nova Edition" to "Nova Edition",
        // 刷写
        "选文件 → 执行" to "Pick a file → run", "安装模块" to "Install module",
        "修补 boot" to "Patch boot", "刷入镜像" to "Flash image",
        "确认刷入" to "Confirm flash", "确认刷入？" to "Confirm flash?",
        // 设置
        "外观 · 壁纸 · 关于" to "Look · Wallpaper · About", "界面" to "Interface",
        "材质" to "Material", "玻璃" to "Glass", "深浅色" to "Theme", "强调色" to "Accent",
        "壁纸" to "Wallpaper", "图片" to "Image", "视频" to "Video", "程序化" to "Procedural",
        "关于" to "About", "跟随系统" to "System", "白色" to "Light", "黑色" to "Dark",
        "始终白色" to "Always light", "始终黑色" to "Always dark",
        "检查与修复" to "Check & fix", "内核" to "Kernel",
        // 体检
        "存在风险" to "At risk", "需要关注" to "Needs attention", "整体健康" to "Healthy",
        "没有发现需要处理的问题" to "No issues found",
        "正在读取内核、模块与检测状态" to "Reading kernel, modules and detection state",
        "看报告" to "Report", "去刷写" to "Flash", "看模块" to "Modules", "补丁中心" to "Patches",
        // 许可
        "运行时组件" to "Runtime", "内置资源" to "Bundled assets", "上游项目" to "Upstream",
        "随 APK 分发的组件与来源" to "Components shipped with the APK",
        "本项目以 GPL-3.0 发布" to "Released under GPL-3.0", "界面渲染" to "UI rendering",
        "基础 Android 能力" to "Android platform APIs", "root shell 交互" to "root shell",
        "内核模块与 ksud 原始实现" to "Kernel module and ksud",
        "本仓库直接上游" to "Direct upstream", "内置动态壁纸" to "Built-in wallpaper",
        "春秋检测脚本" to "Detection script", "一键隐藏套件" to "Hide bundle",
        "维护者提供" to "By maintainer", "模块作者提供" to "By module author",
        // 4.x
        "两条可行路线" to "Two viable paths", "修补现成 boot.img" to "Patch an existing boot.img",
        "编内核补丁" to "Kernel patch", "内核编译流程" to "Kernel build steps",
        "去修补" to "Patch now",
        // 补丁
        "隐藏可调试标记" to "Hide debuggable flag", "还原正式构建标签" to "Restore release tags",
        "构建类型改为 user" to "Build type to user", "归一化构建者信息" to "Normalize builder",
        "伪装已上锁 BL" to "Fake locked bootloader", "伪装 dm-verity 正常" to "Fake dm-verity",
        "伪装 vbmeta 已锁定" to "Fake vbmeta locked", "关闭 adb root 标记" to "Clear adb root",
        "抹掉模拟器特征" to "Hide emulator traits", "恢复 ro.secure" to "Restore ro.secure",
        "重启使补丁失效" to "Reboot to revert", "查看已修改属性" to "Show changed props",
        "系统属性" to "System props", "引导状态" to "Boot state", "调试与环境" to "Debug & env",
        "其他" to "Other", "重启自动恢复" to "Reverts on reboot",
        // 内核设置
        "su 兼容" to "su compat", "内核 umount" to "Kernel umount", "sulog" to "sulog",
        "adb root" to "adb root", "SELinux 隐藏" to "SELinux hide",
        "为传统 su 调用提供兼容层" to "Compat layer for legacy su calls",
        "开机时处理模块的挂载回退" to "Handle module umount at boot",
        "记录 su 调用日志" to "Log su calls", "允许 adb root" to "Allow adb root",
        "对目标进程隐藏 SELinux 状态" to "Hide SELinux state from target",
        // 月份
        "%d月%d日" to "%d/%d",
    )

    fun load(ctx: android.content.Context) {
        val saved = ctx.getSharedPreferences("nova_i18n", android.content.Context.MODE_PRIVATE)
            .getString("lang", Lang.Zh.name)
        lang = runCatching { Lang.valueOf(saved ?: Lang.Zh.name) }.getOrDefault(Lang.Zh)
    }

    fun save(ctx: android.content.Context) {
        ctx.getSharedPreferences("nova_i18n", android.content.Context.MODE_PRIVATE)
            .edit().putString("lang", lang.name).apply()
    }

    fun toggle(ctx: android.content.Context) {
        lang = if (lang == Lang.En) Lang.Zh else Lang.En
        save(ctx)
    }

    fun t(zh: String): String = if (lang == Lang.En) en[zh] ?: zh else zh
}

/** UI 里统一用这个：`Text(S("首页"))`。 */
fun S(zh: String): String = I18n.t(zh)
