#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 5

# 自动维护 TrickyStore 目标列表 (添加常见的强检测应用)
TARGET_FILE="/data/adb/tricky_store/target.txt"
mkdir -p "$(dirname "$TARGET_FILE")"
touch "$TARGET_FILE"
for pkg in "com.google.android.gms" "com.android.vending" "com.tencent.tmgp.sgame" "com.eg.android.AlipayGphone"; do
    if ! grep -q "^$pkg$" "$TARGET_FILE" 2>/dev/null; then
        echo "$pkg" >> "$TARGET_FILE"
    fi
done
