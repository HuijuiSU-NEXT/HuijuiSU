# HuiJuiSU NEXT Ultra v5.0.2

> 修复版：解决内置功能「明明有 root 却提示未就位」的根因。

| | |
|---|---|
| 版本号 | `v5.0.2 (35337)` |
| 基线 | v5.0.1 |
| 包名 | `com.huijui.sunext`（覆盖安装） |
| APK | 5 个架构，37.6 – 49.4 MB |

---

## 一、内置资源误报「未就位」

### 现象

点「一键隐藏 / 安装元模块 / 春秋词条修复」，提示：

```
内置隐藏模块未就位（需要 root 部署）
```

即使已经授予 root、内核 LKM 正常工作，也一直这样。

### 根因：检查文件时没走 root

部署逻辑本身是对的：

```bash
su -c "cp <缓存文件> /data/adb/huijui/"
```

但**检查逻辑用的是应用进程**：

```kotlin
File("/data/adb/huijui/hide_bundle_v1.3.zip").isFile   // 永远 false
```

应用进程对 `/data/adb` 没有读权限，`isFile` 必然返回 `false`
—— 于是「部署成功」也被判定成「没就位」。

### 修复

存在性判断改由 root 执行：

```kotlin
suspend fun remoteExists(path: String): Boolean {
    val out = runAsRoot("test -s '$path' && echo __YES__ || echo __NO__")
    return out.contains("__YES__")
}
```

覆盖三处：

- 一键隐藏模块
- 元模块 Hybrid Mount
- 春秋词条修复脚本

### 失败时会给出真实原因

不再只回一句「需要 root」，而是把部署日志带出来，例如：

```
内置隐藏模块未就位：cp: /data/adb/huijui/...: Permission denied
```

---

## 二、执行方式统一到 libsu

v5.0.1 已把 `HuijuiShellUtils` 从 `ProcessBuilder("su")` 改为 libsu `Shell.cmd`，
本版继续确认所有内置动作都走这条路径：

| 动作 | 执行方式 |
|---|---|
| 内置资源部署 | libsu |
| 一键隐藏安装 | libsu |
| 元模块状态查询 | libsu |
| Evil Service 自查 | libsu |
| 春秋词条修复 | libsu |

原因是 KernelSU 系内核只对「被认可的管理器进程」放行 su 的 exec，
直接 `ProcessBuilder("su")` 会拿到 `EACCES`。

---

## 三、Root 状态卡片

工具箱第一张卡片，一键显示真实数据：

```
code=0 APP_UID=10352 /system/bin/su SU_UID=0 /data/adb/ksu/bin/ksud
```

| 字段 | 含义 |
|---|---|
| `APP_UID` | 应用自身 uid |
| `SU_UID=0` | **su 真的成功**，有这行说明 root 完全正常 |
| ksud 路径 | 文件存在说明内核侧工具在位 |

功能失败但 `SU_UID=0` 有值时，问题在具体命令的权限，而不是 root。

---

## 四、热刷入三态判断（v5.0.1 引入，本版保留）

| 状态 | 判断依据 | 提示 |
|---|---|---|
| `READY` | `modules/hybrid_mount` + `hybrid-mount` 可执行 | 可热刷入 |
| `NEEDS_REBOOT` | 只在 `modules_update/` 里 | 已安装，重启后生效 |
| `MISSING` | 两处都没有 | 未检测到元模块 |

二进制查找顺序：模块根目录 → `binaries/` → 按架构猜测。

---

## 五、升级建议

| 情况 | 建议 |
|---|---|
| 从 v5.0.0 / v5.0.1 升级 | 直接覆盖安装 |
| 首次使用内置功能 | 打开后先看「Root 状态」卡片，确认 `SU_UID=0` |
| 热刷入不可用 | 确认元模块处于 `READY`（装完需重启一次） |

---

## 六、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.2 MB |
| `armeabi-v7a` | 老 32 位设备 | 37.6 MB |
| `x86_64` | 模拟器 / 平板 | 39.6 MB |
| `riscv64` | RISC-V | 38.0 MB |
| `universal` | 不确定选这个 | 49.4 MB |

---

## 七、仍未改变的限制

| 项 | 说明 |
|---|---|
| **热刷入范围** | 仅纯 VFS 文件计划的模块可免重启 |
| **元模块前置** | 需先安装 Hybrid Mount 并重启一次 |
| **开机激活** | 需要先打开一次管理器完成资源落盘 |

---

<sub>完整对比：`git log v5.0.1..v5.0.2`</sub>
