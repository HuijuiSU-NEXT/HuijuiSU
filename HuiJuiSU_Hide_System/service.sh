#!/system/bin/sh
MODDIR=${0%/*}
# 等待启动
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done

# 这里可以加入对于 Zygisk Next 的守护
# 如果你没装 Zygisk Next，下面这行是为了给 LSPosed 腾出挂载位置
mkdir -p /data/adb/modules/huijui_hide_system/zygisk

# 自动检测 Shamiko 是否工作
if [ -f /data/adb/modules/zygisksu/module.prop ]; then
    touch /data/adb/modules/huijui_hide_system/zygisk_enabled
fi
