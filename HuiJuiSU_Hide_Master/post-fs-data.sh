#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 3

# 万能属性清洗 (保留原机指纹，仅洗白调试状态)
resetprop ro.boot.verifiedbootstate "green"
resetprop ro.boot.flash.locked "1"
resetprop ro.boot.vbmeta.device_state "locked"
resetprop ro.debuggable "0"
resetprop ro.build.type "user"
resetprop ro.build.tags "release-keys"
resetprop ro.secureboot.lockstate "locked"
resetprop sys.oem_unlock_allowed "0"

# SUSFS 内核级隐藏
if command -v ksu_susfs >/dev/null 2>&1; then
    ksu_susfs add_sus_mount /data/adb
    ksu_susfs add_sus_path /data/adb/modules
    ksu_susfs add_sus_path /data/adb/ksu
    ksu_susfs enable_hide_loops
    ksu_susfs enable_avc_log_spoofing
fi

# 补全 VerifiedBootHash
VB_HASH=$(getprop ro.boot.vbmeta.digest)
if [ -n "$VB_HASH" ] && [ ! -f /data/adb/VerifiedBootHash/VerifiedBootHash.txt ]; then
    mkdir -p /data/adb/VerifiedBootHash
    echo "$VB_HASH" > /data/adb/VerifiedBootHash/VerifiedBootHash.txt
    chmod 644 /data/adb/VerifiedBootHash/VerifiedBootHash.txt
fi
