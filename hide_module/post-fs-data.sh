#!/system/bin/sh
MODDIR=${0%/*}

# 等待系统属性加载完成
until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 1
done

# --- 1. 隐藏 Root 管理器包名 ---
# 这里可以把你自己或者常见的 Root 包名加进去
HIDE_PKGS="com.huijui.sunext com.huijui.sunext.ultra me.weishu.kernelsu com.topjohnwu.magisk"

for pkg in $HIDE_PKGS; do
  if [ -d "/data/data/$pkg" ]; then
    # 用空目录盖住真实数据目录，让其他 App 读不到
    mkdir -p /data/local/tmp/fake_$pkg
    mount -o bind /data/local/tmp/fake_$pkg /data/data/$pkg
  fi
done

# --- 2. 隐藏 ksu/magisk 相关敏感路径 ---
# 伪装 /data/adb 下的内容
if [ -d "/data/adb/ksu" ]; then
  mkdir -p /data/local/tmp/empty_dir
  mount -o bind /data/local/tmp/empty_dir /data/adb/ksu
fi
if [ -d "/data/adb/magisk" ]; then
  mount -o bind /data/local/tmp/empty_dir /data/adb/magisk
fi
if [ -d "/data/adb/modules" ]; then
  mount -o bind /data/local/tmp/empty_dir /data/adb/modules
fi

# 伪装 /sbin 下的 magisk 挂载点 (老版本检测)
if [ -d "/sbin/.magisk" ]; then
  mount -o bind /data/local/tmp/empty_dir /sbin/.magisk
fi

# --- 3. 拦截常见的检测属性 ---
resetprop ro.boot.verifiedbootstate green
resetprop ro.boot.flash.locked 1
resetprop ro.secureboot.lockstate locked
resetprop ro.debuggable 0
resetprop ro.adb.secure 1

# --- 4. 隐藏 SU 二进制文件 ---
# 用空文件盖住常见的 su 路径
for su_path in /system/bin/su /system/xbin/su /sbin/su /su/bin/su; do
  if [ -e "$su_path" ]; then
    touch /data/local/tmp/fake_su
    mount -o bind /data/local/tmp/fake_su "$su_path"
  fi
done
