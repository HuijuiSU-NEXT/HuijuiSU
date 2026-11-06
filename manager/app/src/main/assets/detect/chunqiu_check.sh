#!/system/bin/sh
# ============================================================================
#  chunqiu_check.sh —— 春秋 Native Check 检测点复刻（自检脚本）
#
#  用法：
#    su -c 'sh /data/local/tmp/chunqiu_check.sh'
#    su -c 'sh /data/local/tmp/chunqiu_check.sh --quiet'    # 只列命中的
#    su -c 'sh /data/local/tmp/chunqiu_check.sh --pid 1234' # 指定被检测进程
#
#  说明（重要）：
#    这个脚本不是把 APK 反编译出来的原样代码——那个 APK 的 dex 是加密的、
#    检测主体在 libchunqiunative.so（17 MB）里且被混淆，没法逐行还原。
#    这里做的是「等效复刻」：按从它 native 库里能提取到的检测点
#    （/proc/self/maps、/proc/self/mountinfo、/proc/self/mounts、
#      /proc/self/attr/current、TracerPid、ptrace、/data/adb、
#      magisk / kernelsu / apatch / zygisk / zygisk_module_entry、
#      Xposed / LSPosed 类名、SELinux 策略上下文）逐项检查。
#
#    因为检测本身是「App 看自己」，所以脚本默认挑一个普通应用进程
#    （uid >= 10000）当被检测对象，读它的 maps / mountinfo / attr/current，
#    这样结论才接近真实；找不到（或你指定 --pid）就用自己。
#
#  退出码：发现的项数（最多 99）
# ============================================================================

QUIET=0
WANT_PID=""
while [ $# -gt 0 ]; do
  case "$1" in
    --quiet) QUIET=1 ;;
    --pid) shift; WANT_PID="$1" ;;
    -h|--help) sed -n '2,30p' "$0" 2>/dev/null; exit 0 ;;
  esac
  shift
done

HITS=0
TOTAL=0
MAXSHOW=6

hr() { printf '%s\n' "------------------------------------------------------------"; }

sec() {
  TOTAL=$((TOTAL + 1))
  printf '\n[%02d] %s\n' "$TOTAL" "$1"
}

hit() { HITS=$((HITS + 1)); printf '  [FOUND] %s\n' "$1"; }
ok() { [ "$QUIET" = "1" ] || printf '  [ ok  ] %s\n' "$1"; }
note() { [ "$QUIET" = "1" ] || printf '  [ note] %s\n' "$1"; }

show_hits() {
  _n=0
  while IFS= read -r _l; do
    [ -n "$_l" ] || continue
    _n=$((_n + 1))
    [ "$_n" -le "$MAXSHOW" ] && printf '         %s\n' "$_l"
    [ "$_n" -gt 200 ] && break
  done
  [ "$_n" -gt "$MAXSHOW" ] && printf '         ...（共 %s 行）\n' "$_n"
  [ "$_n" -eq 0 ] && return 1
  return 0
}

uid_is_root() { [ "$(id -u 2>/dev/null)" = "0" ]; }

# 找一个普通应用进程当「被检测方」
pick_target() {
  if [ -n "$WANT_PID" ] && [ -r "/proc/$WANT_PID/maps" ]; then
    printf '%s' "$WANT_PID"
    return 0
  fi
  for _d in /proc/[0-9]*; do
    [ -r "$_d/status" ] || continue
    [ -r "$_d/maps" ] || continue
    _u=$(grep -m1 '^Uid:' "$_d/status" 2>/dev/null | tr -s ' \t' ' ' | cut -d' ' -f2)
    case "$_u" in ''|*[!0-9]*) continue ;; esac
    [ "$_u" -ge 10000 ] || continue
    printf '%s' "${_d#/proc/}"
    return 0
  done
  return 1
}

printf '春秋 Native Check 检测点复刻 · %s\n' "$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null)"
printf '设备：%s %s / Android %s / SDK %s\n' \
  "$(getprop ro.product.brand 2>/dev/null)" \
  "$(getprop ro.product.model 2>/dev/null)" \
  "$(getprop ro.build.version.release 2>/dev/null)" \
  "$(getprop ro.build.version.sdk 2>/dev/null)"

if uid_is_root; then
  printf '身份：root\n'
else
  printf '身份：普通用户（很多路径读不到，结果仅供参考）\n'
fi

TPID=$(pick_target)
if [ -n "$TPID" ]; then
  printf '被检测进程：/%s（%s）\n' "$TPID" "$(tr '\000' ' ' < "/proc/$TPID/cmdline" 2>/dev/null | cut -c1-60)"
else
  printf '被检测进程：没找到普通应用进程，用自己代替（结果是 root shell 视角，偏严格）\n'
fi

# ---------------------------------------------------------------------------
# 1. 系统属性
# ---------------------------------------------------------------------------
sec "系统属性"
check_prop_is() { # 名 期望值 说明
  _v=$(getprop "$1" 2>/dev/null)
  if [ "$_v" = "$2" ]; then
    ok "$1=$_v"
  else
    hit "$3（$1=$_v，期望 $2）"
  fi
}
check_prop_is ro.debuggable 0 "可调试构建"
check_prop_is ro.secure 1 "ro.secure 非 1"
check_prop_is ro.build.type user "构建类型不是 user"
check_prop_is ro.build.tags release-keys "构建不是 release-keys（test-keys = 自编译/改包）"
check_prop_is ro.boot.verifiedbootstate green "verified boot 非 green（BL 解锁/校验失败）"
check_prop_is ro.boot.flash.locked 1 "BL 未上锁"
check_prop_is ro.boot.veritymode enforcing "dm-verity 非 enforcing"

note "ro.build.fingerprint=$(getprop ro.build.fingerprint 2>/dev/null)"
case "$(getprop ro.kernel.qemu 2>/dev/null)" in 1) hit "ro.kernel.qemu=1（模拟器）" ;; esac
case "$(getprop ro.hardware 2>/dev/null)" in
  goldfish*|ranchu*|vbox*|qemu*) hit "ro.hardware=$(getprop ro.hardware)（模拟器）" ;;
esac

# ---------------------------------------------------------------------------
# 2. root 痕迹
# ---------------------------------------------------------------------------
sec "root 与提权痕迹"
_root_paths="
/system/bin/su
/system/xbin/su
/sbin/su
/su/bin/su
/data/local/su
/data/local/bin/su
/data/local/xbin/su
/system/sd/xbin/su
/system/bin/failsafe/su
/system/bin/daemonsu
/system/xbin/busybox
/data/adb
/data/adb/magisk
/data/adb/magisk.db
/data/adb/magisk_simple
/data/adb/modules
/data/adb/ksu
/data/adb/ksud
/data/adb/ksu/bin/ksud
/data/adb/ap
/data/adb/apd
/data/adb/zygisksu
/data/adb/lspd
/data/adb/omk
/data/adb/tricky_store
/sbin/.magisk
/dev/.magisk
/cache/.disable_magisk
/data/magisk
/init.magisk.rc
/debug_ramdisk
"
_root_hit=0
for _p in $_root_paths; do
  if [ -e "$_p" ]; then
    hit "存在 $_p"
    _root_hit=$((_root_hit + 1))
  fi
done
[ "$_root_hit" -eq 0 ] && ok "常见 su / magisk / ksu 路径都没找到"

command -v magisk >/dev/null 2>&1 && hit "PATH 里有 magisk（$(command -v magisk 2>/dev/null)）"
command -v ksud >/dev/null 2>&1 && hit "PATH 里有 ksud（$(command -v ksud 2>/dev/null)）"
if [ -d /data/adb/modules ]; then
  _m=$(ls /data/adb/modules 2>/dev/null | head -n 8 | tr '\n' ' ')
  [ -n "$_m" ] && hit "/data/adb/modules 非空：$_m"
fi

# ---------------------------------------------------------------------------
# 3. 挂载表
# ---------------------------------------------------------------------------
sec "挂载信息（mountinfo / mounts）"
_VIEW="/proc/self"
[ -n "$TPID" ] && _VIEW="/proc/$TPID"

_mount_hit=0
if [ -r "$_VIEW/mountinfo" ]; then
  _out=$(grep -Ei 'magisk|zygisk|ksu|apatch|worker|/data/adb|riru|lsposed' "$_VIEW/mountinfo" 2>/dev/null)
  if [ -n "$_out" ]; then
    hit "mountinfo 里有可疑挂载"
    printf '%s\n' "$_out" | show_hits
    _mount_hit=1
  fi
  _out=$(grep -E ' /(system|vendor|product|system_ext) ' "$_VIEW/mountinfo" 2>/dev/null | grep -Ei 'overlay|tmpfs')
  if [ -n "$_out" ]; then
    hit "/system 等分区被 overlay/tmpfs 挂载（模块注入的典型特征）"
    printf '%s\n' "$_out" | show_hits
    _mount_hit=1
  fi
fi
[ "$_mount_hit" -eq 0 ] && ok "mountinfo 没看到 magisk/overlay 痕迹"

if [ -n "$TPID" ] && [ -r /proc/1/mountinfo ] && [ -r "$_VIEW/mountinfo" ]; then
  _a=$(grep -c . "$_VIEW/mountinfo" 2>/dev/null)
  _b=$(grep -c . /proc/1/mountinfo 2>/dev/null)
  if [ -n "$_a" ] && [ -n "$_b" ] && [ "$_a" != "$_b" ]; then
    hit "应用与 init 的挂载条目数不同（$TPID=$_a / init=$_b），可能存在「只对某些进程隐藏」"
  fi
fi

# ---------------------------------------------------------------------------
# 4. maps
# ---------------------------------------------------------------------------
sec "内存映射（$_VIEW/maps）"
if [ -r "$_VIEW/maps" ]; then
  _out=$(grep -Ei 'magisk|zygisk|frida|xposed|lspd|riru|substrate|kernelsu|apatch|/data/adb' "$_VIEW/maps" 2>/dev/null)
  if [ -n "$_out" ]; then
    hit "maps 里有可疑 so / 路径"
    printf '%s\n' "$_out" | show_hits
  else
    ok "maps 里没有 magisk/zygisk/xposed/frida 字样"
  fi

  _rwx=$(grep -E '^[0-9a-f]+-[0-9a-f]+ rwxp' "$_VIEW/maps" 2>/dev/null | grep -v -E '\[|/dev/|/system/|/apex/' | head -n 4)
  if [ -n "$_rwx" ]; then
    hit "存在匿名的 rwx 可执行段（可能是注入/hook）"
    printf '%s\n' "$_rwx" | show_hits
  else
    ok "没有匿名 rwx 段"
  fi
else
  note "$_VIEW/maps 读不到"
fi

# ---------------------------------------------------------------------------
# 5. 调试 / 进程 / ptrace
# ---------------------------------------------------------------------------
sec "调试与进程"
if [ -r "$_VIEW/status" ]; then
  _tp=$(grep -m1 '^TracerPid:' "$_VIEW/status" 2>/dev/null | tr -dc '0-9')
  if [ -n "$_tp" ] && [ "$_tp" != "0" ]; then
    hit "被检测进程正在被 ptrace（TracerPid=$_tp）"
  else
    ok "TracerPid=0"
  fi
fi

_out=""
for _d in /proc/[0-9]*; do
  [ -r "$_d/cmdline" ] || continue
  _c=$(tr '\000' ' ' < "$_d/cmdline" 2>/dev/null)
  [ -n "$_c" ] || continue
  case "$_c" in
    *frida*|*magiskd*|*ksud*|*zygiskd*|*lspd*|*xposed*|*riru*)
      _out="$_out
${_d#/proc/} $_c" ;;
  esac
done
if [ -n "$_out" ]; then
  hit "有可疑进程在跑"
  printf '%s\n' "$_out" | sed '/^$/d' | show_hits
else
  ok "没有 frida/magiskd/ksud/zygiskd 之类的进程"
fi

if [ -r /proc/net/tcp ]; then
  if grep -qE ':(69A2|69A3) 00000000:0000 0A' /proc/net/tcp 2>/dev/null; then
    hit "本地有 27042/27043 监听（frida-server 默认端口）"
  else
    ok "没有 frida 默认端口监听"
  fi
fi

# ---------------------------------------------------------------------------
# 6. SELinux
# ---------------------------------------------------------------------------
sec "SELinux"
_enforce=$(getenforce 2>/dev/null)
_ctx=$(cat "$_VIEW/attr/current" 2>/dev/null)
note "getenforce=$_enforce"
note "被检测进程上下文：$_ctx"
case "$_enforce" in
  Permissive|Disabled) hit "SELinux 是 $_enforce" ;;
esac
case "$_ctx" in
  *magisk*|*ksu*|*u:r:su:s0*) hit "进程上下文是 root 域：$_ctx" ;;
  *u:r:shell:s0*) note "进程上下文是 shell 域（不是普通 App，参考性打折）" ;;
esac

# ---------------------------------------------------------------------------
# 7. Zygisk / LSPosed
# ---------------------------------------------------------------------------
sec "Zygisk / LSPosed / Xposed"
_z=0
for _f in /data/adb/modules/*/zygisk/*.so; do
  [ -f "$_f" ] || continue
  hit "存在 zygisk 模块 so：$_f"
  _z=$((_z + 1))
  [ "$_z" -ge 3 ] && break
done
[ -e /data/adb/zygisksu ] && hit "存在 /data/adb/zygisksu"
[ -e /data/adb/lspd ] && hit "存在 /data/adb/lspd（LSPosed）"

if [ -r "$_VIEW/maps" ]; then
  _x=$(grep -Ei 'lsposed|xposed|lspd|riru' "$_VIEW/maps" 2>/dev/null)
  [ -n "$_x" ] && { hit "maps 里有 LSPosed/Xposed 字样"; printf '%s\n' "$_x" | show_hits; }
fi

# ---------------------------------------------------------------------------
# 8. 模拟器
# ---------------------------------------------------------------------------
sec "模拟器 / 容器"
_emu=0
for _f in /dev/qemu_pipe /dev/goldfish_pipe /sys/qemu_trace \
          /system/lib/libc_malloc_debug_qemu.so /system/lib64/libc_malloc_debug_qemu.so \
          /system/bin/nox-prop /system/lib/libnoxd.so; do
  [ -e "$_f" ] && { hit "存在 $_f"; _emu=1; }
done
case "$(getprop ro.product.board 2>/dev/null)" in
  goldfish*|ranchu*|vbox*) hit "ro.product.board=$(getprop ro.product.board)（模拟器）"; _emu=1 ;;
esac
[ "$_emu" -eq 0 ] && ok "没找到模拟器特征"

# ---------------------------------------------------------------------------
# 9. 系统完整性
# ---------------------------------------------------------------------------
sec "系统完整性"
if [ -w /system ] || [ -w /system/bin ]; then
  hit "/system 可写（正常机器不会）"
else
  ok "/system 只读"
fi
[ -w /vendor ] && hit "/vendor 可写" || ok "/vendor 只读"
[ "$(getprop ro.boot.verifiedbootstate 2>/dev/null)" = "orange" ] && hit "ro.boot.verifiedbootstate=orange（BL 解锁）"

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
hr
printf '共检查 %d 组，命中 %d 项。\n' "$TOTAL" "$HITS"
if [ "$HITS" -eq 0 ]; then
  printf '结论：这套环境在「同类检测」下比较干净。\n'
else
  printf '结论：有 %d 项特征能被同类检测看到，逐条看上面的 [FOUND]。\n' "$HITS"
fi
printf '提示：本脚本是「等效复刻」，不等于春秋那份 native 检测的逐条结果；\n'
printf '      它反映的是落盘 / 挂载 / maps / 进程 / 属性 / SELinux 这几类特征。\n'

[ "$HITS" -gt 99 ] && HITS=99
exit "$HITS"
