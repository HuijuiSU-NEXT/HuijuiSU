MODDIR=${0%/*}

cd "$MODDIR" || exit

mkdir -p /data/adb/zygisksu
[ -f /data/adb/zygisksu/znctx ] && mv /data/adb/zygisksu/znctx /data/adb/zygisksu/znctx.old
[ -f /data/adb/zygisksu/modules_info ] && mv /data/adb/zygisksu/modules_info /data/adb/zygisksu/modules_info.old

if [ -d /data/adb/ksu/log ]; then
  [ -f /data/adb/ksu/log/znctx ] && mv /data/adb/ksu/log/znctx /data/adb/ksu/log/znctx.old
  [ -f /data/adb/ksu/log/modules_info ] && mv /data/adb/ksu/log/modules_info /data/adb/ksu/log/modules_info.old
fi

export ZYGISK_ENABLED
[ -f /data/adb/zygisksu/klog ] && [ "1" = "$(cat /data/adb/zygisksu/klog)" ] && export KLOG_ENABLED=1
./bin/zygiskd daemon

if [ -d /data/adb/ksu/log ]; then
  cp /data/adb/zygisksu/znctx /data/adb/ksu/log/znctx
  cp /data/adb/zygisksu/modules_info /data/adb/ksu/log/modules_info
fi
