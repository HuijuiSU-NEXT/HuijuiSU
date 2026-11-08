#!/system/bin/sh
# HuiJuiSU 核心内置 —— post-fs-data
#
# 只做两件事，且都很快：
#   1. 首次开机应用隐藏默认值（之后用标记文件跳过）
#   2. 让元模块能在本次开机就参与挂载（如果它已经装好）
#
# 这里不做任何安装动作，避免拖慢开机；安装放在 boot-completed。

H=/data/adb/huijui
K=/data/adb/ksud
LOG=$H/boot.log

[ -d "$H" ] || exit 0

log() { echo "[$(date '+%m-%d %H:%M:%S')] $*" >> "$LOG" 2>/dev/null; }

[ -x "$K" ] || K=/data/adb/ksu/bin/ksud
[ -x "$K" ] || { log "ksud 不存在，跳过"; exit 0; }

# ---- 1. 隐藏默认值：只在第一次生效 ----
if [ ! -f "$H/.hidden_applied" ]; then
  log "应用隐藏默认值"
  $K sus_path add '/data/local/tmp' >/dev/null 2>&1
  $K sus_path add '/data/adb/ksu' >/dev/null 2>&1
  $K sus_path add '/data/adb/ksud' >/dev/null 2>&1
  $K sus_path add '/data/adb/modules' >/dev/null 2>&1
  $K sus_path add '/data/adb/modules_update' >/dev/null 2>&1
  $K sus_path add '/data/adb/magisk' >/dev/null 2>&1
  $K sus_kstat add '/data/local/tmp' normal >/dev/null 2>&1
  $K sus_kstat add '/data/adb/ksu' normal >/dev/null 2>&1
  $K hide_sus_mnts_for_non_su_procs add >/dev/null 2>&1
  $K avc_log_spoofing add >/dev/null 2>&1
  $K feature save >/dev/null 2>&1
  touch "$H/.hidden_applied"
  log "隐藏默认值完成"
fi

# ---- 2. 元模块存在就让它在本次开机生效 ----
if [ -d /data/adb/modules/hybrid_mount ] && [ ! -f /data/adb/modules/hybrid_mount/disable ]; then
  log "hybrid_mount 已就绪"
fi

exit 0
