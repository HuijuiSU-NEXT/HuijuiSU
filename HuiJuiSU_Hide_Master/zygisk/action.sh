set -e

print_log() {
    if [ "$FROM_WEBUI" != "1" ]; then
        echo $*
    fi
}

send_bugreport() {
    su 2000 -c "am start -a android.intent.action.SEND --eu android.intent.extra.STREAM content://com.android.shell/bugreports/$1 -t '*/*' --grant-read-uri-permission"
}

if [ "$FROM_WEBUI" == "1" ] && [ "$1" == "--send" ]; then
    send_bugreport "$2"
    exit $?
fi

print_log "Generating Bugreport ..."
stamp=$(date +%Y%m%d-%H%M%S)
tmp=/data/local/tmp/zygisknext-bugreport-$$

has_shell=false
SHELL_DIR='/data/user_de/0/com.android.shell'
if [ -d $SHELL_DIR ]; then
    outdir=$SHELL_DIR/files/bugreports
    mkdir -p $SHELL_DIR/files/bugreports
    chown 2000 $SHELL_DIR/files
    chgrp 2000 $SHELL_DIR/files
    chown 2000 $SHELL_DIR/files/bugreports
    chgrp 2000 $SHELL_DIR/files/bugreports
    rm $outdir/ZygiskNext-bugreport-*.tar.gz 2>/dev/null || true
    has_shell=true
else
    outdir=/sdcard/Download
fi

filename=ZygiskNext-bugreport-$stamp.tar.gz
out=$outdir/$filename

rm -rf "$tmp"
mkdir -p "$tmp" /sdcard/Download

print_log "- Collecting basic information ..."
cat <<-EOF > $tmp/basic.txt
Kernel: $(uname -r)
SDK: $(getprop ro.build.version.sdk)
SDK_FULL: $(getprop ro.build.version.sdk_full)
Fingerprint: $(getprop ro.build.fingerprint)

======== zn status =======
$(/data/adb/modules/zygisksu/bin/zygiskd status)

======== zn module.prop ======
$(cat /data/adb/modules/zygisksu/module.prop)
EOF

for src in /data/adb/zygisksu/bugreports /data/adb/zygisksu/bugreports.old /data/tombstones /data/system/dropbox /data/adb/ksu/log /data/adb/ap/log /sys/fs/pstore; do 
    if [ -e "$src" ]; then 
        print_log "- Adding $src"
        cp -a "$src" "$tmp/"
    fi
done
print_log "- Collecting logs ..."
logcat -b all -d -f "$tmp/logcat.log"
print_log "- Compressing ..."
/system/bin/tar -czf "$out" -C "$tmp" .
rm -rf "$tmp"
print_log "Bugreport generated at $out"

if $has_shell ; then
    print_log "Trying to share ..."
    chown 2000 $out
    chgrp 2000 $out
    if [ "$FROM_WEBUI" != "1" ]; then
        send_bugreport $filename
        sleep 100
    fi
fi

if [ "$FROM_WEBUI" == "1" ]; then
    echo "$out"
fi
