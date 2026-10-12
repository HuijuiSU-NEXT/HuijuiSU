# HuijuiSU
> 基于 BakaSU 深度定制，已领先上游 9 个 commits (forked from [Baka-SU/BakaSU](https://github.com/Baka-SU/BakaSU))。

**English** | [简体中文](./zh/README.md)

HuijuiSU 是一款基于 [BakaSU](https://github.com/Baka-SU/BakaSU) 深度定制的 Android Root 管理器，保留了 BakaSU 稳定的底层核心，并针对国内玩机环境进行了大量 UI 重构与功能增强。

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part License：GPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

## ✨ HuijuiSU 核心特性 (独占功能)

1. **一键体检**：系统健康状态一目了然，包括电量、温度、Root 状态、安全补丁等。
2. **一键备份 / 救砖**：支持备份当前槽位 `boot` / `init_boot` / `vbmeta`，一键生成 PC 端 `restore.bat` fastboot 脚本，拒绝变砖。
3. **SUSFS 隐身中心**：集成 SUSFS 完整配置，一键开启全隐身（隐藏挂载点、Kernel Umount、AVC 伪装等），对抗各类毒瘤检测。
4. **重构前端 UI**：重写管理器交互界面，更加美观直观。

---

## 🧩 继承自 BakaSU 的底层特性

1. 基于内核的 `su` 和 Root 权限管理
2. 基于 [metamodules](https://kernelsu.org/guide/metamodule.html) 的模块系统
3. [App Profile](https://kernelsu.org/guide/app-profile.html)：将 Root 权限关进笼子里
4. 支持非 GKI 和 GKI 1.0
5. 支持多管理器（官方 KernelSU / KOWSU 可作为 BakaSU 内核的管理器）

## ⚙️ 兼容性状态

- 官方支持 Android GKI 2.0 设备 (内核 5.10+)。
- 旧内核 (3.4+) 也兼容，但需要自行编译内核。
- 目前仅支持 `arm64-v8a`、`armeabi-v7a` 和 `X86_64`。
- 内置的 [SUSFS](https://gitlab.com/simonpunk/susfs4ksu) **仅**支持回移植到内核 4.3+。
- `Tracepoint Syscall Redirect hook` 仅支持 GKI2 (5.10+) 内核。

## 🪝 Hook 模式

- `Tracepoint Syscall Redirect hook`：默认钩子模式，来自上游，仅支持 `arm64-v8a` 或 `x86_64` ABI 的 GKI2 内核。
- `Manual Hook`：兼容性最强的钩子，支持 Linux 内核 3.4 到 6.18。
- `SUSFS Inline Hook`：来自 [SuSFS](https://github.com/simonpunk/susfs4ksu) 的钩子，类似于 `Manual Hook`，但由 `SUSFS` 项目提供。

## 📜 许可协议

- `kernel` 目录下的文件遵循 [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) 协议。
- 美术和视觉文件（如 `BakaSU_Clean.svg` 等）由 [OukaroMF](https://github.com/OukaroMF) 创作，采用分层许可协议，详见 [`ASSETS_LICENSE.md`](../ASSETS_LICENSE.md)。
- 除上述文件或目录外，所有其他部分遵循 [GPL-3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) 协议。

## 🙏 致谢

- [KernelSU](https://github.com/tiann/KernelSU): 上游基础
- [BakaSU](https://github.com/Baka-SU/BakaSU): 本项目基于此深度定制
- [SukiSU-Ultra/SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra): Fork 来源
- [Magisk](https://github.com/topjohnwu/Magisk): 强大的 Root 工具
- [genuine](https://github.com/brevent/genuine/): APK v2 签名验证