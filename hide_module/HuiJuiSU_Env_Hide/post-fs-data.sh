#!/system/bin/sh
MODDIR=${0%/*}

# 等待系统属性加载完成
until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 1
done

# 1. 隐藏 Root 包名数据目录
HIDE_PKGS="com.huijui.sunext com.huijui.sunext.ultra me.weishu.kernelsu com.topjohnwu.magisk"
for pkg in $HIDE_PKGS; do
  if [ -d "/data/data/$pkg" ]; then
    mkdir -p /data/local/tmp/fake_$pkg
    mount -o bind /data/local/tmp/fake_$pkg /data/data/$pkg
  fi
done

# 2. 隐藏敏感路径
mkdir -p /data/local/tmp/empty_dir
if [ -d "/data/adb/ksu" ]; then mount -o bind /data/local/tmp/empty_dir /data/adb/ksu; fi
if [ -d "/data/adb/magisk" ]; then mount -o bind /data/local/tmp/empty_dir /data/adb/magisk; fi
if [ -d "/sbin/.magisk" ]; then mount -o bind /data/local/tmp/empty_dir /sbin/.magisk; fi

# 3. 严格模式专属处理 (清理 /proc/mounts 痕迹)
if [ -f "$MODDIR/strict_mode.on" ]; then
    # 通过屏蔽挂载点防扫描 (需要内核支持)
    umount -l /data/adb/modules 2>/dev/null
fi

# 4. 系统属性伪装
resetprop ro.boot.verifiedbootstate green
resetprop ro.boot.flash.locked 1
resetprop ro.secureboot.lockstate locked
resetprop ro.debuggable 0
resetprop ro.adb.secure 1
resetprop ro.build.type user
resetprop ro.build.tags release-keys

# 5. 隐藏 SU 路径
touch /data/local/tmp/fake_su
for su_path in /system/bin/su /system/xbin/su /sbin/su /su/bin/su; do
  if [ -e "$su_path" ]; then
    mount -o bind /data/local/tmp/fake_su "$su_path"
  fi
done
