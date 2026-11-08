#!/system/bin/sh
# ============================================================================
#  evil_service_check.sh —— 春秋「Evil Service」自查 / 清理
#
#  依据：mingzun09/Chunqiu-Detector-Problem-solution 里 Evil Service 的
#        4.6.2 实测判据（四层，只有第一层参与裁决）：
#    1) 关键词扫目录（命中即报）：
#         Sui      : shamiko Shamiko 内核 调度 suius suidaemon ssssss suidaem
#         SandHook : sandhook SandHook sandcore
#         Pumpcin  : pumpcin Pumpcin taiwan Taiwan
#         EDXposed : tricky Tricky
#       扫描目录：/dev/binderfs /dev /sys/fs/selinux /proc/self/fd /apex
#                 /system/framework /data/misc /data/system
#                 /data/local/tmp /storage/emulated/0/ /data/
#    2) /dev/binderfs 目录项数 > 3 → 报 Sui（最容易误报的一条）
#    3) /proc/net/unix、fd 命中只影响详情，不影响判定
#    4) 详情里 LSPosed 恒为 NO；命中 LSPosed 实际走的是 tricky → EDXposed
#
#  用法：
#    su -c 'sh evil_service_check.sh'            # 只检查
#    su -c 'sh evil_service_check.sh --clean'    # 删除 /data/local/tmp 下的残留
#
#  退出码：命中项数
# ============================================================================

CLEAN=0
[ "$1" = "--clean" ] && CLEAN=1

HITS=0
hit() { HITS=$((HITS + 1)); printf '  [HIT ] %s\n' "$1"; }
ok()  { printf '  [ ok ] %s\n' "$1"; }
inf() { printf '  [info] %s\n' "$1"; }

# Sui 族关键词（含中文，注意 sh 里按空格分词，中文两个字之间不要加空格）
SUI_KW="shamiko Shamiko 内核 调度 suius suidaemon ssssss suidaem"
SAN_KW="sandhook SandHook sandcore"
PUM_KW="pumpcin Pumpcin taiwan Taiwan"
EDX_KW="tricky Tricky"

SCAN_DIRS="/dev/binderfs /dev /sys/fs/selinux /proc/self/fd /apex /system/framework /data/misc /data/system /data/local/tmp /storage/emulated/0 /data"

echo "============================================================"
echo " 春秋 Evil Service 自查 · $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"

# ---------------------------------------------------------------------------
# 1. /dev/binderfs 计数（>3 = Sui，最容易误报）
# ---------------------------------------------------------------------------
echo ""
echo "[1] /dev/binderfs"
if [ -d /dev/binderfs ]; then
  _n=$(ls -A /dev/binderfs 2>/dev/null | wc -l)
  _list=$(ls -A /dev/binderfs 2>/dev/null | tr '\n' ' ')
  inf "条目数：$_n"
  inf "内容：$_list"
  if [ "$_n" -gt 3 ]; then
    hit "binderfs 条目 $_n > 3 → 按判据算 Sui（社区公认易误报，很多正常机也会到 4+）"
  else
    ok "binderfs 条目 $_n ≤ 3"
  fi
else
  inf "/dev/binderfs 不存在（这条查不了）"
fi

# ---------------------------------------------------------------------------
# 2. 关键词扫目录
# ---------------------------------------------------------------------------
echo ""
echo "[2] 关键词扫描"

scan_one() { # $1 目录  $2 关键词  $3 族名
  _dir="$1"
  [ -d "$_dir" ] || return 0
  _found=""
  for _kw in $2; do
    _hits=$(ls -A "$_dir" 2>/dev/null | grep -F -i -- "$_kw" 2>/dev/null)
    [ -n "$_hits" ] || continue
    _found="$_found
      $_kw → $(printf '%s' "$_hits" | tr '\n' ' ')"
  done
  # /data/local/tmp 再多看一层（模块残留常在子目录里）
  if [ "$_dir" = "/data/local/tmp" ]; then
    for _kw in $2; do
      _hits=$(ls -A /data/local/tmp/*/ 2>/dev/null | grep -F -i -- "$_kw" 2>/dev/null)
      [ -n "$_hits" ] || continue
      _found="$_found
      $_kw(子目录) → $(printf '%s' "$_hits" | tr '\n' ' ')"
    done
  fi
  if [ -n "$_found" ]; then
    hit "$3 @ $_dir$_found"
  fi
}

for _d in $SCAN_DIRS; do
  scan_one "$_d" "$SUI_KW" "Sui"
  scan_one "$_d" "$SAN_KW" "SandHook"
  scan_one "$_d" "$PUM_KW" "Pumpcin"
  scan_one "$_d" "$EDX_KW" "EDXposed"
done

# ---------------------------------------------------------------------------
# 3. 只影响详情、不影响判定的两路（跑一下方便对照）
# ---------------------------------------------------------------------------
echo ""
echo "[3] 详情用：unix socket / fd"
if [ -r /proc/net/unix ]; then
  _u=$(grep -Ei 'lsposed|sui|sandhook|pumpcin' /proc/net/unix 2>/dev/null | head -n 5)
  if [ -n "$_u" ]; then
    inf "unix socket 命中（只进详情）："
    printf '%s\n' "$_u" | sed 's/^/         /'
  else
    inf "unix socket 无命中"
  fi
fi
_fd=$(ls -l /proc/self/fd 2>/dev/null | grep -Ei 'lsposed|sui|sandhook|pumpcin' | head -n 5)
[ -n "$_fd" ] && { inf "fd 命中（只进详情）："; printf '%s\n' "$_fd" | sed 's/^/         /'; }

# ---------------------------------------------------------------------------
# 4. 常见残留（/data/local/tmp）
# ---------------------------------------------------------------------------
echo ""
echo "[4] /data/local/tmp 残留"
_LEFT=""
for _pat in sandhook SandHook pumpcin Pumpcin shamiko Shamiko suidaemon suius suidaem ssssss; do
  for _p in /data/local/tmp/$_pat*; do
    [ -e "$_p" ] || continue
    _LEFT="$_LEFT $_p"
  done
done
if [ -n "$_LEFT" ]; then
  hit "有残留：$_LEFT"
  if [ "$CLEAN" = "1" ]; then
    for _p in $_LEFT; do
      rm -rf "$_p" 2>/dev/null && echo "        已删除 $_p"
    done
    inf "清理完请重启再复扫"
  else
    inf "加 --clean 可以删掉这些"
  fi
else
  ok "/data/local/tmp 没有上述残留"
fi

# ---------------------------------------------------------------------------
# 5. 其它常见触发点（附录 B 里和本条相关的）
# ---------------------------------------------------------------------------
echo ""
echo "[5] 其它容易连带命中的路径"
for _p in /data/local/tmp/resetprop /data/local/tmp/mount_mask /data/local/tmp/mount_mark \
          /data/local/tmp/simpleHook /data/system/tricky_store /data/misc/tricky_store \
          /data/adb/tricky_store; do
  [ -e "$_p" ] && inf "存在 $_p（tricky 关键词族 / 异常文件族，按需处理）"
done

echo ""
echo "============================================================"
if [ "$HITS" -eq 0 ]; then
  echo " 没扫到：说明这条要么是 binderfs 计数误报，要么是概率命中"
else
  echo " 命中 $HITS 项，按上面 [HIT] 逐条处理"
fi
echo " 判据来源：https://mingzun09.github.io/Chunqiu-Detector-Problem-solution/"
echo "============================================================"

[ "$HITS" -gt 99 ] && HITS=99
exit "$HITS"
