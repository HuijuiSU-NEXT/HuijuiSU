#!/sbin/sh
SKIPUNZIP=0

ui_print " "
ui_print "*********************************************"
ui_print "      HuiJuiSU Ultimate Hide 安装程序"
ui_print "*********************************************"
ui_print " "

# 音量键选择函数
chooseport() {
    [ "$1" ] && local delay=$1 || local delay=60
    local error=false
    while true; do
        timeout $delay /system/bin/getevent -lqc 1 2>&1 > /data/local/tmp/events & sleep 0.5
        if (grep -q 'KEY_VOLUMEUP *DOWN' /data/local/tmp/events); then
            return 0
        elif (grep -q 'KEY_VOLUMEDOWN *DOWN' /data/local/tmp/events); then
            return 1
        fi
    done
}

# 1. 检测已有的 Zygisk Next 模块
ZYNEXT_DIR="/data/adb/modules/zygisksu"
ZYNEXT_ALT_DIR="/data/adb/modules/zygisk_next"

if [ -d "$ZYNEXT_DIR" ] || [ -d "$ZYNEXT_ALT_DIR" ]; then
    ui_print "⚠️ 警告：检测到手机已安装 Zygisk Next！"
    ui_print "  咱们模块内置了最新的 Zygisk Next 引擎。"
    ui_print "  为了防止冲突卡开机，必须移除旧版本。"
    ui_print " "
    ui_print "  [音量上键] 自动删除旧版并继续安装"
    ui_print "  [音量下键] 取消安装（保持现状）"
    
    if chooseport 30; then
        ui_print "- 正在清理旧版 Zygisk Next..."
        rm -rf "$ZYNEXT_DIR"
        rm -rf "$ZYNEXT_ALT_DIR"
        ui_print "- ✅ 旧版清理完毕，继续安装！"
    else
        ui_print " "
        ui_print "❌ 用户选择取消。安装中止。"
        abort "请手动卸载旧版 Zygisk Next 后再试！"
    fi
else
    ui_print "✅ 未检测到冲突的 Zygisk Next，准备就绪。"
fi

# 2. 检查 KernelSU 自带的 Zygisk 是否开启 (提示用户去关)
KSU_ZYGISK_STATUS=$(getprop persist.sys.zygisk.enabled)
if [ "$KSU_ZYGISK_STATUS" = "true" ]; then
    ui_print " "
    ui_print "⚠️ 严重警告：检测到 KernelSU 自带的 Zygisk 正在运行！"
    ui_print "  刷入后请务必重启，并在管理器设置中关闭自带的 Zygisk！"
    ui_print "  否则本模块的 Zygisk Next 会与其冲突导致黑屏！"
fi

ui_print " "
ui_print "- 正在安装 HuiJui 核心组件..."

# 赋予底层脚本执行权限
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/post-fs-data.sh 0 0 0755
set_perm $MODPATH/service.sh 0 0 0755

ui_print "- ✅ 安装成功！请重启手机以生效。"
