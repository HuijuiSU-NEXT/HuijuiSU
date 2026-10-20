if [ -f /data/adb/modules/zygisksu/disable ]; then
  cat /data/adb/modules/zygisksu/module.prop.orig > /data/adb/modules/zygisksu/module.prop
  rm -rf /data/adb/modules/zn_magisk_compat
  rm /data/adb/service.d/.zn_cleanup.sh
fi
