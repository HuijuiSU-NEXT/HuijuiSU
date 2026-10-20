#!/system/bin/sh
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 1; done
sleep 5

# 自动维护 TrickyStore 的 keybox 与目标列表
TARGET_FILE="/data/adb/tricky_store/target.txt"
mkdir -p "$(dirname "$TARGET_FILE")"
touch "$TARGET_FILE"

# 添加主流游戏和金融App
for pkg in "com.google.android.gms" "com.android.vending" "com.tencent.tmgp.sgame" "com.tencent.tmgp.pubgmhd" "com.eg.android.AlipayGphone" "com.tencent.mm"; do
    if ! grep -q "^$pkg$" "$TARGET_FILE" 2>/dev/null; then
        echo "$pkg" >> "$TARGET_FILE"
    fi
done
