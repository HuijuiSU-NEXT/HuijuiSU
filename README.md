<div align="center">

# HuiJuiSU NEXT Ultra

**基于 KernelSU 的 Root 管理器 · 内置隐藏 · 真热刷入 · 开机秒激活**

[![Release](https://img.shields.io/github/v/release/HuiJui-forge/HuijuiSU?label=release)](https://github.com/HuiJui-forge/HuijuiSU/releases/latest)
[![License](https://img.shields.io/github/license/HuiJui-forge/HuijuiSU)](LICENSE)

[官网](https://huijui-forge.github.io/) · [下载](https://github.com/HuiJui-forge/HuijuiSU/releases/latest) · [安装文档](https://huijui-forge.github.io/install.html) · [FAQ](https://huijui-forge.github.io/faq.html) · [TG 群](https://t.me/Huijui123) · [TG 频道](https://t.me/HuijuiSU) · [QQ 群](https://qm.qq.com/q/5pS7RgzhJe)

</div>

---

## 这是什么

HuiJuiSU 是 **KernelSU 的下游分支**。保留上游的管理器与内核能力，额外把隐藏模块、检测对抗、热刷入与一批实用工具收进管理器。

和上游的区别不在于「多了几个开关」，而在于：

- 隐藏所需组件**随 APK 内置**，不用自己找包、导配置
- 模块安装后可以**免重启热挂载**（纯 VFS 计划）
- 重启后隐藏配置**自动激活**，不需要手动打开管理器

---

## 核心特性

| 能力 | 说明 |
|---|---|
| **模块管理** | 安装 / 启停 / 卸载 / 备份还原，支持真热刷入 |
| **超级用户** | 自动列出全部应用，一键授权 / 撤销，真实应用图标 |
| **内置一键隐藏** | 隐藏模块随 APK 分发，首次启动自动部署，安装前清理旧版本 |
| **真·热刷入** | 接入 Hybrid Mount runtime，纯 VFS 模块装完即挂载 |
| **开机秒激活** | `huijui_core` 在开机过程中完成隐藏配置与依赖安装 |
| **极客工具箱** | 12 张卡片：boot 备份、SystemUI 重启、缓存清理、Evil 自查、春秋修复等 |
| **核心防误删** | 内置隐藏模块禁止卸载与停用 |
| **功耗优化** | 背景模糊限流 30fps 并后台停止；隐藏规则合并为一次 su |
| **稳定性** | root 调用带超时并串行化，不再无限转圈；崩溃日志落盘 |

---

## 内置资源

随 APK 分发，首次启动自动部署到 `/data/adb/huijui/`：

| 资源 | 用途 |
|---|---|
| `hide_bundle_v1.4_super.zip` | 一键隐藏模块（超级定制版，26 MB） |
| `hybrid_mount_6.2.5.zip` | Hybrid Mount 元模块（热刷入依赖） |
| `evil_service_check.sh` | Evil Service 自查脚本 |
| `chunqiu_fix_v1.zip` | 春秋词条修复脚本 |

部署幂等：版本未变且文件齐全时跳过；版本变化自动覆盖，并清理设备上的旧包。

---

## 工具箱

| 卡片 | 作用 |
|---|---|
| Root 状态 | 显示真实 UID 与 su 输出，排错第一步 |
| 备份 boot 分区 | 导出到 `Download/boot_backup_*.img` |
| 重启 SystemUI | 状态栏卡死时自救 |
| 清理内存缓存 | `drop_caches` 释放 RAM |
| 清理临时文件 | `/data/local/tmp` 与模块安装残留 |
| 模块备份与还原 | 打包 / 恢复已装模块 |
| 一键隐藏 | 安装内置隐藏模块并尝试热激活 |
| Evil Service 自查 | 扫描后给出结论 |
| 元模块状态 | 查询 Hybrid Mount 运行时 |
| 安装元模块 | 缺失时一键安装 |
| 春秋词条修复 | 二次确认后执行内置脚本 |
| 重启设备 | 二次确认 |

每张卡片都有可见执行状态，150 秒超时会明确提示，不会停在「正在…」。

---

## 安装

> 先确认内核类型，选错是「装了却没有 root」的最常见原因。完整步骤见 [安装文档](https://huijui-forge.github.io/install.html)。

| 内核 | 方式 | 说明 |
|---|---|---|
| GKI 2.0（5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18） | **LKM** | 下载对应 `kernelsu.ko` 加载，不用重编内核 |
| 4.x / 非 GKI 魔改内核 | **内核补丁** | 需要内核源码重新编译 boot |

### 快速开始（GKI）

```bash
adb push aarch64-android16-6.12_kernelsu.ko /data/local/tmp/
adb shell su -c "ksud insmod /data/local/tmp/aarch64-android16-6.12_kernelsu.ko"
adb shell su -c "lsmod | grep -i ksu"
adb install HuiJuiSU_NEXT_Ultra_arm64-v8a-release.apk
```

装好后打开 App → 工具箱 → 一键隐藏。

---

## 兼容性

| KMI | Android | LKM | 状态 |
|---|---|---|---|
| `android12-5.10` | 12 | ✅ | 支持 |
| `android13-5.10` | 13 | ✅ | 支持 |
| `android13-5.15` | 13 | ✅ | 支持 |
| `android14-5.15` | 14 | ✅ | 支持 |
| `android14-6.1` | 14 | ✅ | 支持 |
| `android15-6.6` | 15 | ✅ | 支持 |
| `android16-6.12` | 16 | ✅ | 支持 |
| `android17-6.18` | 17 | ✅ | 支持 |
| 4.x / 非 GKI | 8–13 | ❌ | 内核补丁 |

---

## 版本线

| 版本 | 主题 |
|---|---|
| v5.0.0 | 大版本：内置资源、真热刷入、功耗与启动优化 |
| v5.0.1 – v5.0.2 | libsu 执行修复、root 侧文件判断 |
| v5.0.3 | 版本号修复、元模块检测 |
| v5.0.4 | 春秋修复、壁纸稳定 |
| v5.0.6 | 内置隐藏模块 v1.4 超级定制版 |
| v5.0.8 | 资源部署改 root 流式写入 |
| v5.0.10 – v5.0.11 | 清理旧模块、禁止卸载与停用 |
| **v5.0.12** | **root 调用超时与串行化，修复卡在加载中** |

完整更新见 [Releases](https://github.com/HuiJui-forge/HuijuiSU/releases)。

---

## 目录结构

```
kernel/         内核补丁与内核模块
userspace/      ksud 等用户态程序
manager/        Android 管理器 App
site/           官网（GitHub Pages）
docs/           文档与发布说明
scripts/        构建与工具脚本
```

## 构建

```bash
cd manager
./gradlew :app:assembleRelease
```

内核与 ksud 的构建见 `.github/workflows/`。

---

## 相关仓库

| 仓库 | 说明 |
|---|---|
| [Snow-love-Module](https://github.com/HuiJui-forge/Snow-love-Module) | 一键隐藏模块源码 |
| [Huijui_Module_Repo](https://github.com/HuiJui-forge/Huijui_Module_Repo) | 模块仓库源 |
| [HuiJui-Toolbox](https://github.com/HuiJui-forge/HuiJui-Toolbox) | 工具箱 |

---

## 贡献者

- [hhcdxii-ship-it](https://github.com/hhcdxii-ship-it) —— 内核 / 管理器 / 发布
- [Yuyan9178345](https://github.com/Yuyan9178345) —— 隐藏模块（[Snow-love-Module](https://github.com/HuiJui-forge/Snow-love-Module)）

完整名单见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

---

## 交流

| 渠道 | 链接 |
|---|---|
| Telegram 群 | https://t.me/Huijui123 |
| Telegram 频道 | https://t.me/HuijuiSU |
| QQ 群 | https://qm.qq.com/q/5pS7RgzhJe |

---

## 免责声明

本项目用于**个人设备的环境隔离与隐私保护**，请勿用于规避合法风控。
刷机有风险，操作前请备份 boot 分区。

## 许可

[GPL-3.0](LICENSE)。
