MODDIR=${0%/*}

if [ ! -f /data/adb/service.d/.zn_cleanup.sh ]; then
  mkdir -p /data/adb/service.d
  cat "$MODDIR/cleanup.sh" > /data/adb/service.d/.zn_cleanup.sh
  chmod +x /data/adb/service.d/.zn_cleanup.sh
fi

if [ "$ZYGISK_ENABLED" = "1" ]; then
  exit 0
fi

/data/adb/modules/zygisksu/bin/zygiskd service-stage
