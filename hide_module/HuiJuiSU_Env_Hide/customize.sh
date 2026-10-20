SKIPUNZIP=0

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

ui_print "- 欢迎安装 HuiJuiSU 深度隐藏模块"
ui_print "- 请选择运行模式："
ui_print "  [音量上键] 严格模式 (深度清理挂载痕迹)"
ui_print "  [音量下键] 兼容模式 (保留基础隐藏)"

if chooseport 30; then
    touch $MODPATH/strict_mode.on
    ui_print "- 已选择：严格模式"
else
    ui_print "- 已选择：兼容模式"
fi

# 赋予脚本执行权限
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/post-fs-data.sh 0 0 0755
