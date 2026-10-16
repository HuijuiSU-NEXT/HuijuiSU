# HuiJuiSU NEXT

> 基于 [BakaSU](https://github.com/Baka-SU/BakaSU) 深度定制的 Android Root 管理器
> 保留 BakaSU 稳定底层，针对国内玩机环境做了大量 UI 重构与功能增强。

**English** | [简体中文](./zh/README.md)

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part License：GPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

---

## ✨ HuiJuiSU NEXT 核心特性

### 独立工具 Hub

底部导航重新设计为 **5 个 tab**：主页 / **工具** / 超级用户 / 模块 / 设置。

工具功能从主页独立出来，避免信息密度过高，也让工具更容易被发现。

### 7 大实用工具

| 功能 | 说明 |
|---|---|
| **一键体检** | 电量 / 温度 / BL / 内核 / Root / SELinux / 存储 |
| **性能模式** | 省电 / 均衡 / 性能 / 游戏，四档切换系统调度 |
| **一键备份包** | boot + init_boot + vbmeta + 模块快照，附带 restore.bat |
| **一键救砖** | 从备份目录一键 dd 刷回，可选还原模块状态 |
| **GKI 内核下载** | built-in 模式专用的社区预编译内核跳转 |
| **刷入定制内核** | 支持 `.img`（built-in）和 `.ko`（LKM）双格式 |
| **应用管理器** | 冻结 / 解冻 / 强停 / 清数据 / 卸载（当前用户） |

### 内置隐藏（默认最强）

HuiJuiSU NEXT 采用**装完即用**的隐藏策略：

- **路径隐藏**：8 个关键 `sus_path`（ksu / ksud / modules / magisk / tmp 等）
- **挂载隐藏**：`hide_sus_mnts_for_non_su_procs` 隐藏 sus 挂载点
- **日志伪装**：`avc_log_spoofing` 隐藏 SELinux 拒绝日志
- **内核统计隐藏**：`sus_kstat` 覆盖 `/system` 和 `/vendor`
- **自动应用**：App 启动时后台静默执行，规则更新后自动重新应用

### 模块快照

备份时自动记录 `/data/adb/modules/` 下所有模块的启用 / 卸载状态到 `modules.json`。

救砖时勾选"同时还原模块状态"，自动把模块恢复到备份时的配置，**不用手动逐个重新启用**。

### 定制内核刷入

针对 GKI 设备的 built-in 模式痛点：

- **`.img` 格式**：Root 权限 dd 刷入 + vbmeta 校验处理（防回滚）+ 可选清 LKM 残留
- **`.ko` 格式**：调用 `ksud boot-patch -f -m` 自动注入
- **智能识别**：自动检测文件类型和大小，防止刷错

---

## 🧩 继承自 BakaSU 的底层特性

1. 基于内核的 `su` 和 Root 权限管理
2. 基于 [metamodules](https://kernelsu.org/guide/metamodule.html) 的模块系统
3. [App Profile](https://kernelsu.org/guide/app-profile.html)：将 Root 权限关进笼子里
4. 支持非 GKI 和 GKI 1.0
5. 支持多管理器（官方 KernelSU / KOWSU / BakaSU）

---

## ⚙️ 兼容性状态

- 官方支持 Android **GKI 2.0** 设备（内核 5.10+）
- 旧内核（3.4+）也兼容，但需自行编译内核
- 支持架构：`arm64-v8a`、`armeabi-v7a`、`x86_64`
- 内置 [SUSFS](https://gitlab.com/simonpunk/susfs4ksu) **仅支持**回移植到内核 4.3+
- `Tracepoint Syscall Redirect hook` 仅支持 GKI2（5.10+）内核

---

## 🪝 Hook 模式

- **`Tracepoint Syscall Redirect hook`**：默认钩子模式，来自上游，仅支持 `arm64-v8a` 或 `x86_64` ABI 的 GKI2 内核
- **`Manual Hook`**：兼容性最强的钩子，支持 Linux 内核 3.4 到 6.18
- **`SUSFS Inline Hook`**：来自 [SuSFS](https://github.com/simonpunk/susfs4ksu) 的钩子，类似 `Manual Hook`

---

## 🔧 性能优化

### 内核热路径日志

v3.0.0 把内核高频调用路径的 `pr_info` 全部转为 `pr_debug`，包括：

- `syscall_hook.c`（arm64/x86_64/riscv64）：37 处
- `ksud_integration.c`：18 处
- `setuid_hook.c`、`sucompat.c`、`dispatch.c`、`supercall.c` 等：80+ 处

### sulog 快速返回

`ksu_sulog_emit_grant_root` 加快速返回，sulog 关闭时每次 `setresuid` 省掉后续逻辑。

---

## ⚠️ 升级提示

### 包名变更

- **旧包名**：`com.huijui.manager` / `com.huijuisu.huijuisu`
- **新包名**：`com.huijui.sunext`

从旧版升级需要先卸载旧版再装新版。

### 升级后状态

| 项目 | 状态 |
|---|---|
| Root 授权列表 | ⚠️ 会清空，需重新授权 |
| 模块启用状态 | ✅ 保留 |
| SUSFS 配置 | ✅ 保留 |
| 备份历史 | ✅ 保留 |

---

## 📜 许可协议

- `kernel/` 目录下的文件遵循 [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) 协议
- 美术和视觉文件由 [OukaroMF](https://github.com/OukaroMF) 创作，采用分层许可协议，详见 [`ASSETS_LICENSE.md`](../ASSETS_LICENSE.md)
- 除上述文件或目录外，所有其他部分遵循 [GPL-3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) 协议

---

## 🙏 致谢

- [KernelSU](https://github.com/tiann/KernelSU): 上游基础
- [BakaSU](https://github.com/Baka-SU/BakaSU): 本项目的基础
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra): Fork 来源
- [Magisk](https://github.com/topjohnwu/Magisk): 强大的 Root 工具
- [liquid-glass](https://github.com/nadeemiqbal/liquid-glass): 液态玻璃 UI 库
- [genuine](https://github.com/brevent/genuine/): APK v2 签名验证

---

## 💬 反馈

- GitHub Issues: https://github.com/hhcdxii-ship-it/HuijuiSU/issues
- QQ 群：应用内 About 页面点击"加入 QQ 群"
