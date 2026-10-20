#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done

# 1. 内核级隐藏 (SUSFS)
if command -v ksu_susfs >/dev/null 2>&1; then
    ksu_susfs add_sus_mount /data/adb
    ksu_susfs add_sus_path /data/adb/modules
    ksu_susfs add_sus_path /data/adb/ksu
    ksu_susfs enable_hide_loops
    ksu_susfs enable_avc_log_spoofing
    ksu_susfs set_cmdline "androidboot.verifiedbootstate=green"
    ksu_susfs set_uname "5.10.168-android12-9-00001-g0e5c9c5c6f76" "user" "release-keys"
fi

# 2. 补全 VerifiedBootHash
if [ ! -f /data/adb/VerifiedBootHash/VerifiedBootHash.txt ] && [ -n "$(getprop ro.boot.vbmeta.digest)" ]; then
    mkdir -p /data/adb/VerifiedBootHash
    getprop ro.boot.vbmeta.digest > /data/adb/VerifiedBootHash/VerifiedBootHash.txt
fi

# 3. 属性伪装
resetprop ro.boot.verifiedbootstate green
resetprop ro.boot.flash.locked 1
resetprop ro.boot.vbmeta.device_state locked
resetprop ro.debuggable 0
resetprop ro.adb.secure 1
resetprop ro.build.tags release-keys
