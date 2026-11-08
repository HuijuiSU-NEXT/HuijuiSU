# HuiJuiSU NEXT Ultra v5.0.1

> 修复版：解决「明明有 root，内置功能却提示未授权」和「热刷入永远说没装元模块」两个实际卡点。

| | |
|---|---|
| 版本号 | `v5.0.1 (35335)` |
| 基线 | v5.0.0 |
| 包名 | `com.huijui.sunext`（覆盖安装） |
| APK | 5 个架构，37.6 – 49.4 MB |

---

## 一、内置功能误报「没有 root」

### 现象

v5.0.0 上，工具箱里从「一键隐藏」开始往后的卡片全部提示失败：

```
失败：请确认已授予 Root
```

但实际上内核是 LKM 正常工作，前面的「备份 boot」「重启 SystemUI」都正常。

### 根因

分界线正好对上了执行方式：

| 卡片 | 执行方式 | 结果 |
|---|---|---|
| 前 5 张 | libsu `Shell.cmd` | ✅ 正常 |
| 一键隐藏及之后 | `ProcessBuilder("su", "-c", …)` | ❌ 被内核拒绝 |

KernelSU 系内核只对**被认可的管理器进程**放行 `su` 的 exec，
应用进程直接 `ProcessBuilder("su")` 会拿到 `EACCES`；
而 libsu 走的是管理器自己建立的 root shell（`ksud debug su`），不受这个限制。

### 修复

`HuijuiShellUtils` 全部改走 libsu：

```kotlin
val result = Shell.cmd(cmd).exec()   // 不再 ProcessBuilder("su")
```

覆盖范围：一键隐藏、Evil Service 自查、元模块状态、安装元模块、春秋词条修复。

### 顺带改掉的误导文案

```diff
- "失败：请确认已授予 Root"
+ "失败：命令没有成功（先看上面 Root 状态卡）"
```

新增「Root 状态」卡片，一键显示真实数据：

```
code=0 APP_UID=10352 /system/bin/su SU_UID=0 /data/adb/ksu/bin/ksud
```

只要 `SU_UID=0` 出现，就说明 root 完全正常，问题在别的环节。

---

## 二、热刷入永远提示「未加载元模块」

### 现象

元模块 Hybrid Mount 已经装好，热刷入仍然提示：

```
元模块未安装：装好 Hybrid Mount 后即可热激活
```

### 根因一：只查了 `modules/`，没查 `modules_update/`

KernelSU 安装模块后**先落到 `/data/adb/modules_update/`，重启才会移入 `/data/adb/modules/`**。
刚装完还没重启时，旧代码查 `modules/` 自然查不到 → 误判成「没装」。

### 根因二：二进制路径写错

Hybrid Mount 的 `customize.sh` 在安装时会把二进制从 `binaries/` 移到模块根目录：

```bash
BIN_TARGET="$MODPATH/hybrid-mount"
rm -rf "$MODPATH/binaries"
```

旧代码找的是 `binaries/hybrid-mount-arm64`，永远不存在。

### 修复：改成三态判断

| 状态 | 判断依据 | 用户看到的提示 |
|---|---|---|
| `READY` | `modules/hybrid_mount/module.prop` 存在且 `hybrid-mount` 可执行 | 可热刷入 |
| `NEEDS_REBOOT` | 只在 `modules_update/` 里 | **元模块已安装，重启后生效** |
| `MISSING` | 两处都没有 | 未检测到元模块 Hybrid Mount |

二进制查找顺序也放宽了：

```
/hybrid-mount  →  /binaries/hybrid-mount-<arch>  →  modules_update 下的同名文件
```

---

## 三、升级建议

| 你的情况 | 建议 |
|---|---|
| 装了 v5.0.0，内置功能报错 | 直接覆盖安装 v5.0.1 |
| 刚装完元模块还没重启 | 重启一次，之后热刷入即可用 |
| 想用热刷入 | 元模块必须处于 `READY`（已重启生效）状态 |

---

## 四、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.2 MB |
| `armeabi-v7a` | 老 32 位设备 | 37.6 MB |
| `x86_64` | 模拟器 / 平板 | 39.6 MB |
| `riscv64` | RISC-V | 38.0 MB |
| `universal` | 不确定选这个 | 49.4 MB |

覆盖安装即可，数据保留。

---

## 五、仍未改变的限制

| 项 | 说明 |
|---|---|
| **热刷入范围** | 只有纯 VFS 文件计划的模块能免重启；Magic / Overlay 后端仍需重启 |
| **元模块前置** | 必须先装 Hybrid Mount 并重启一次，`READY` 后热刷入才可用 |
| **开机激活** | 需要先打开一次管理器，完成资源落盘与 `huijui_core` 写入 |
| **安全模式 / KPM** | 内核层能力，管理器未实现 |

---

<sub>完整对比：`git log v5.0.0..v5.0.1`</sub>
