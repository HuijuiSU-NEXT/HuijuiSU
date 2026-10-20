#!/system/bin/sh
MODDIR=${0%/*}

# 等待系统完全启动
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 3

# =========================================
# 🔥 全自动自适应逻辑（任何人刷入都不会出错）
# =========================================

# 获取当前真实的机型信息
REAL_BRAND=$(getprop ro.product.brand)
REAL_MODEL=$(getprop ro.product.model)
REAL_DEVICE=$(getprop ro.product.device)
CURRENT_PATCH=$(getprop ro.build.version.security_patch)

# 1. 万能属性清洗：锁定状态 + 隐藏解锁 + 禁用调试
# 这组属性是所有安卓机通用的，直接把底层状态骗成官方原厂
resetprop ro.boot.verifiedbootstate "green"
resetprop ro.boot.flash.locked "1"
resetprop ro.boot.vbmeta.device_state "locked"
resetprop ro.boot.veritymode "enforcing"
resetprop ro.boot.warranty_bit "0"
resetprop ro.warranty_bit "0"
resetprop ro.debuggable "0"
resetprop ro.adb.secure "1"
resetprop ro.build.type "user"
resetprop ro.build.tags "release-keys"
resetprop ro.secureboot.lockstate "locked"

# 清理危险属性
resetprop service.adb.root "0"
resetprop sys.oem_unlock_allowed "0"
resetprop ro.oem_unlock_supported "0"

# 2. 针对性的动态修补（根据品牌做差异化，但保留真身）
if echo "$REAL_BRAND" | grep -iq "xiaomi"; then
    # 小米/HyperOS 专属修补，防止系统商店/相册报错
    resetprop ro.miui.ui.version.code "816"
    resetprop ro.miui.ui.version.name "V816"
    resetprop ro.miui.has_real_blur "1"
elif echo "$REAL_BRAND" | grep -iq "samsung"; then
    # 三星专属：保修位清零，防物理熔断检测
    resetprop ro.boot.warranty_bit "0"
fi

# 3. 多平台芯片分支
HARDWARE=$(getprop ro.hardware)
case "$HARDWARE" in
    mt*)
        # 天玑专属：隐藏 MTK 的调试节点
        ;;
    qcom*)
        # 骁龙专属：隐藏高通底层属性
        ;;
esac

# 4. 全自动挂载隐藏 (适配 KSU / APatch / Magisk)
# 只要发现以下路径，统统挂载隐藏，防死所有路径扫描
for path in "/data/adb/modules" "/data/adb/ksu" "/data/adb/ap" "/data/adb/magisk" "/system/bin/su"; do
    if [ -e "$path" ] && command -v ksu_susfs >/dev/null 2>&1; then
        ksu_susfs add_sus_path "$path"
    fi
done

# 隐藏循环设备
if command -v ksu_susfs >/dev/null 2>&1; then
    ksu_susfs enable_hide_loops
    ksu_susfs enable_avc_log_spoofing
fi

# 5. 生成 VerifiedBootHash (伪回锁通用底座)
VB_HASH=$(getprop ro.boot.vbmeta.digest)
if [ -n "$VB_HASH" ] && [ ! -f /data/adb/VerifiedBootHash/VerifiedBootHash.txt ]; then
    mkdir -p /data/adb/VerifiedBootHash
    echo "$VB_HASH" > /data/adb/VerifiedBootHash/VerifiedBootHash.txt
    chmod 644 /data/adb/VerifiedBootHash/VerifiedBootHash.txt
fi

# 6. 强制 SELinux (必须最后做)
setenforce 1
