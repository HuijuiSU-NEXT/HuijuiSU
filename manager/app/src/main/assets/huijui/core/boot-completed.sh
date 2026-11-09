#!/system/bin/sh
# HuiJuiSU 核心内置 —— boot-completed
#
# 开机完成后再装模块，避免阻塞开机。
# 幂等：已存在就跳过，标记文件防止重复尝试。

H=/data/adb/huijui
K=/data/adb/ksud
LOG=$H/boot.log

[ -d "$H" ] || exit 0

log() { echo "[$(date '+%m-%d %H:%M:%S')] $*" >> "$LOG" 2>/dev/null; }

[ -x "$K" ] || K=/data/adb/ksu/bin/ksud
[ -x "$K" ] || { log "ksud 不存在，跳过安装"; exit 0; }

installed() {
  [ -f "/data/adb/modules/$1/module.prop" ] || [ -d "/data/adb/modules_update/$1" ]
}

# ---- 1. 元模块（Hybrid Mount）：其它模块的挂载基础，优先 ----
if ! installed hybrid_mount; then
  if [ -f "$H/hybrid_mount_6.2.5.zip" ] && [ ! -f "$H/.hybrid_tried" ]; then
    log "安装元模块 hybrid_mount"
    $K module install "$H/hybrid_mount_6.2.5.zip" >> "$LOG" 2>&1
    touch "$H/.hybrid_tried"
  fi
else
  [ -f "$H/.hybrid_tried" ] || touch "$H/.hybrid_tried"
fi

# ---- 2. 一键隐藏模块：等元模块就位后再装 ----
if ! installed hide_bundle; then
  if [ -f "$H/hide_bundle_v1.4.zip" ] && [ ! -f "$H/.hide_tried" ] && installed hybrid_mount; then
    log "安装隐藏模块 hide_bundle"
    $K module install "$H/hide_bundle_v1.4.zip" >> "$LOG" 2>&1
    touch "$H/.hide_tried"
  fi
else
  [ -f "$H/.hide_tried" ] || touch "$H/.hide_tried"
fi

log "boot-completed 完成"
exit 0
