# shellcheck disable=SC2034
SKIPUNZIP=1

DEBUG=False
MIN_KSU_VERSION=10940
MIN_KSUD_VERSION=11575
MIN_MAGISK_VERSION=26402
MIN_APATCH_VERSION=10700

if [ "$BOOTMODE" ] && [ "$KSU" ]; then
  ui_print "- Installing from KernelSU app"
  ui_print "- KernelSU version: $KSU_KERNEL_VER_CODE (kernel) + $KSU_VER_CODE (ksud)"
  if ! [ "$KSU_KERNEL_VER_CODE" ] || [ "$KSU_KERNEL_VER_CODE" -lt "$MIN_KSU_VERSION" ]; then
    ui_print "*********************************************************"
    ui_print "! KernelSU version is too old!"
    ui_print "! Please update KernelSU to latest version"
    abort    "*********************************************************"
  fi
  if ! [ "$KSU_VER_CODE" ] || [ "$KSU_VER_CODE" -lt "$MIN_KSUD_VERSION" ]; then
    ui_print "*********************************************************"
    ui_print "! ksud version is too old!"
    ui_print "! Please update KernelSU Manager to latest version"
    abort    "*********************************************************"
  fi
  if [ "$(which magisk)" ]; then
    ui_print "*********************************************************"
    ui_print "! Multiple root implementation is NOT supported!"
    ui_print "! Please uninstall Magisk before installing Zygisk Next"
    abort    "*********************************************************"
  fi
elif [ "$BOOTMODE" ] && [ "$APATCH" ]; then
  ui_print "- Installing from APatch app"
  if ! [ "$APATCH_VER_CODE" ] || [ "$APATCH_VER_CODE" -lt "$MIN_APATCH_VERSION" ]; then
    ui_print "*********************************************************"
    ui_print "! Apatch version is too old!"
    ui_print "! Please update Apatch to latest version"
    abort    "*********************************************************"
  fi
elif [ "$BOOTMODE" ] && [ "$MAGISK_VER_CODE" ]; then
  ui_print "- Installing from Magisk app"
  if [ "$MAGISK_VER_CODE" -lt "$MIN_MAGISK_VERSION" ]; then
    ui_print "*********************************************************"
    ui_print "! Magisk version is too old!"
    ui_print "! Please update Magisk to latest version"
    abort    "*********************************************************"
  fi
else
  ui_print "*********************************************************"
  ui_print "! Install from recovery is not supported"
  ui_print "! Please install from KernelSU or Magisk app"
  abort    "*********************************************************"
fi

VERSION=$(grep_prop version "${TMPDIR}/module.prop")
ui_print "- Installing Zygisk Next $VERSION"

# check android
if [ "$API" -lt 26 ]; then
  ui_print "! Unsupported sdk: $API"
  abort "! Minimal supported sdk is 26 (Android 8.0)"
else
  ui_print "- Device sdk: $API"
fi

# check architecture
if [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x64" ] && [ "$ARCH" != "arm" ] && [ "$ARCH" != "riscv64" ]; then
  abort "! Unsupported platform: $ARCH"
else
  ui_print "- Device platform: $ARCH"
fi

ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print "*********************************************************"
  ui_print "! Unable to extract verify.sh!"
  ui_print "! This zip may be corrupted, please try downloading again"
  abort    "*********************************************************"
fi
. "$TMPDIR/verify.sh"
extract "$ZIPFILE" 'customize.sh'  "$TMPDIR/.vunzip"
extract "$ZIPFILE" 'verify.sh'     "$TMPDIR/.vunzip"
extract "$ZIPFILE" 'sepolicy.rule' "$TMPDIR"

if [ "$KSU" ]; then
  ui_print "- Checking SELinux patches"
  if ! check_sepolicy "$TMPDIR/sepolicy.rule"; then
    ui_print "*********************************************************"
    ui_print "! Unable to apply SELinux patches!"
    ui_print "! Your kernel may not support SELinux patch fully"
    abort    "*********************************************************"
  fi
fi

ui_print "- Extracting module files"
extract "$ZIPFILE" 'module.prop'     "$MODPATH"
extract "$ZIPFILE" 'post-fs-data.sh' "$MODPATH"
extract "$ZIPFILE" 'service.sh'      "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh'    "$MODPATH"
extract "$ZIPFILE" 'mazoku'          "$MODPATH"
extract "$ZIPFILE" 'cleanup.sh'      "$MODPATH"
extract "$ZIPFILE" 'action.sh'       "$MODPATH"
extract "$ZIPFILE" 'emulated-soft-reboot.sh' "$MODPATH"
mv "$TMPDIR/sepolicy.rule" "$MODPATH"
cp "$MODPATH/module.prop" "$MODPATH/module.prop.orig"

mkdir "$MODPATH/bin"
unzip -o "$ZIPFILE" "webroot/*" -x "*.sha256" -d "$MODPATH"

HAS32BIT=false
if [ -n "$(getprop ro.product.cpu.abilist32)" ] || [ -n "$(getprop ro.system.product.cpu.abilist32)" ]; then
  HAS32BIT=true
fi

if [ "$ARCH" = "riscv64" ]; then
  # Android defines no RV32 companion ABI.
  HAS32BIT=false
  ui_print "- Extracting RISC-V libraries"
  extract "$ZIPFILE" 'bin/riscv64/zygiskd' "$MODPATH/bin" true
  mv "$MODPATH/bin/zygiskd" "$MODPATH/bin/zygiskd64"
  mkdir "$MODPATH/lib64"
  extract "$ZIPFILE" 'lib/riscv64/libzygisk.so' "$MODPATH/lib64" true
  extract "$ZIPFILE" 'lib/riscv64/libpayload.so' "$MODPATH/lib64" true
  extract "$ZIPFILE" 'lib/riscv64/libzn_loader.so' "$MODPATH/lib64" true
  ln -s "./zygiskd64" "$MODPATH/bin/zygiskd"
elif [ "$ARCH" = "x64" ]; then
  if [ "$HAS32BIT" = "true" ]; then
    ui_print "- Extracting x86 libraries"
    extract "$ZIPFILE" 'bin/x86/zygiskd' "$MODPATH/bin" true
    mv "$MODPATH/bin/zygiskd" "$MODPATH/bin/zygiskd32"
    mkdir "$MODPATH/lib"
    extract "$ZIPFILE" 'lib/x86/libzygisk.so' "$MODPATH/lib" true
    extract "$ZIPFILE" 'lib/x86/libzn_loader.so' "$MODPATH/lib" true
  fi

  ui_print "- Extracting x64 libraries"
  extract "$ZIPFILE" 'bin/x86_64/zygiskd' "$MODPATH/bin" true
  mv "$MODPATH/bin/zygiskd" "$MODPATH/bin/zygiskd64"
  mkdir "$MODPATH/lib64"
  extract "$ZIPFILE" 'lib/x86_64/libzygisk.so' "$MODPATH/lib64" true
  extract "$ZIPFILE" 'lib/x86_64/libpayload.so' "$MODPATH/lib64" true
  extract "$ZIPFILE" 'lib/x86_64/libzn_loader.so' "$MODPATH/lib64" true
  ln -s "./zygiskd64" "$MODPATH/bin/zygiskd"
else
  if [ "$ARCH" = "arm" ] || [ "$HAS32BIT" = "true" ]; then
    ui_print "- Extracting arm libraries"
    extract "$ZIPFILE" 'bin/armeabi-v7a/zygiskd' "$MODPATH/bin" true
    mv "$MODPATH/bin/zygiskd" "$MODPATH/bin/zygiskd32"
    mkdir "$MODPATH/lib"
    extract "$ZIPFILE" 'lib/armeabi-v7a/libzygisk.so' "$MODPATH/lib" true
    extract "$ZIPFILE" 'lib/armeabi-v7a/libzn_loader.so' "$MODPATH/lib" true
  fi

  if [ "$ARCH" = "arm64" ]; then
    ui_print "- Extracting arm64 libraries"
    extract "$ZIPFILE" 'bin/arm64-v8a/zygiskd' "$MODPATH/bin" true
    mv "$MODPATH/bin/zygiskd" "$MODPATH/bin/zygiskd64"
    mkdir "$MODPATH/lib64"
    extract "$ZIPFILE" 'lib/arm64-v8a/libzygisk.so' "$MODPATH/lib64" true
    extract "$ZIPFILE" 'lib/arm64-v8a/libpayload.so' "$MODPATH/lib64" true
    extract "$ZIPFILE" 'lib/arm64-v8a/libzn_loader.so' "$MODPATH/lib64" true
    ln -s "./zygiskd64" "$MODPATH/bin/zygiskd"
  else
    extract "$ZIPFILE" 'lib/armeabi-v7a/libpayload.so' "$MODPATH/lib" true
    ln -s "./zygiskd32" "$MODPATH/bin/zygiskd"
  fi
fi

MARCH=$ARCH
if [ "$ARCH" != "arm" ] && [ "$HAS32BIT" = true ]; then
  MARCH="${MARCH}_32"
fi

extract "$ZIPFILE" "machikado.$MARCH" "$MODPATH" true
mv "$MODPATH/machikado.$MARCH" "$MODPATH/machikado"

ui_print "- Setting permissions"
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$MODPATH/bin" 0 0 0755 0755

ui_print "- Ensure config dir created"
mkdir -p /data/adb/zygisksu

ui_print "- Clean up unused configure files"
rm /data/adb/zygisksu/tango 2> /dev/null
rm /data/adb/zygisksu/auto_umount 2> /dev/null

ui_print "- Install cleanup script"
mkdir -p /data/adb/service.d
cat "$MODPATH/cleanup.sh" > /data/adb/service.d/.zn_cleanup.sh
chmod +x /data/adb/service.d/.zn_cleanup.sh

if [ "$KSU" ]; then
  ui_print "- Install znctl for KernelSU"
  rm /data/adb/ksu/bin/zygisk-ctl 2>/dev/null
  rm /data/adb/ksu/bin/znctl 2>/dev/null
  ln -s /data/adb/modules/zygisksu/bin/zygiskd /data/adb/ksu/bin/znctl
elif [ "$APATCH" ]; then
  ui_print "- Install znctl for APatch"
  rm /data/adb/ap/bin/zygisk-ctl 2>/dev/null
  rm /data/adb/ap/bin/znctl 2>/dev/null
  ln -s /data/adb/modules/zygisksu/bin/zygiskd /data/adb/ap/bin/znctl
fi

# Magisk: znctl will be installed by zn magisk compat
