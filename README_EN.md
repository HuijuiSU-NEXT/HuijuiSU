# HuiJuiSU NEXT

> A deeply customized Android Root manager based on [BakaSU](https://github.com/Baka-SU/BakaSU)
> Preserves BakaSU's stable core while featuring extensive UI refactoring and functional enhancements tailored for the domestic modding environment.

[English](./README_EN.md) | [简体中文](./README.md) | [日本語](./README_JA.md) | [한국어](./README_KO.md) | [Русский](./README_RU.md)

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part License：GPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

---

## ✨ HuiJuiSU NEXT Core Features

### Standalone Tool Hub

The bottom navigation has been redesigned into **5 tabs**: Home / **Tools** / Superuser / Modules / Settings.

Tools are separated from the home page to avoid high information density and make them easier to discover.

### 7 Powerful Tools

| Feature | Description |
|---|---|
| **One-Tap Health Check** | Battery / Temperature / Bootloader / Kernel / Root / SELinux / Storage |
| **Performance Modes** | Power Saving / Balanced / Performance / Gaming, four system scheduling profiles |
| **One-Tap Backup Package** | boot + init_boot + vbmeta + module snapshots, with restore.bat included |
| **One-Tap Unbrick** | One-click dd flash back from backup directory, optional module state restore |
| **GKI Kernel Download** | Community pre-compiled kernel redirects specifically for built-in mode |
| **Flash Custom Kernel** | Supports both `.img` (built-in) and `.ko` (LKM) formats |
| **App Manager** | Freeze / Unfreeze / Force Stop / Clear Data / Uninstall (current user) |

### Built-in Hiding (Strongest by Default)

HuiJuiSU NEXT uses a **ready-out-of-the-box** hiding strategy:

- **Path Hiding**: 8 critical `sus_path` entries (ksu / ksud / modules / magisk / tmp, etc.)
- **Mount Hiding**: `hide_sus_mnts_for_non_su_procs` hides sus mount points
- **Log Spoofing**: `avc_log_spoofing` hides SELinux denial logs
- **Kernel Stats Hiding**: `sus_kstat` overrides `/system` and `/vendor`
- **Auto-apply**: Silently executed in the background on app launch, and automatically reapplied after rule updates

### Module Snapshots

Backup automatically records the enable/uninstall status of all modules under `/data/adb/modules/` to `modules.json`.

When unbricking, checking "Also restore module state" automatically restores modules to the configuration at the time of backup, **eliminating the need to manually re-enable them one by one**.

### Custom Kernel Flashing

Addressing the pain points of built-in mode on GKI devices:

- **`.img` Format**: dd flash with Root privileges + vbmeta verification handling (anti-rollback) + optional clear LKM residue
- **`.ko` Format**: Calls `ksud boot-patch -f -m` for automatic injection
- **Smart Recognition**: Automatically detects file type and size to prevent flashing the wrong file

---

## 🧩 Core Features Inherited from BakaSU

1. Kernel-based `su` and Root permission management
2. Module system based on [metamodules](https://kernelsu.org/guide/metamodule.html)
3. [App Profile](https://kernelsu.org/guide/app-profile.html): Lock Root privileges in a cage
4. Supports non-GKI and GKI 1.0
5. Supports multiple managers (Official KernelSU / KOWSU / BakaSU)

---

## ⚙️ Compatibility Status

- Officially supports Android **GKI 2.0** devices (Kernel 5.10+)
- Older kernels (3.4+) are also compatible, but require compiling the kernel yourself
- Supported architectures: `arm64-v8a`, `armeabi-v7a`, `x86_64`
- Built-in [SUSFS](https://gitlab.com/simonpunk/susfs4ksu) **only supports** backports to kernel 4.3+
- `Tracepoint Syscall Redirect hook` only supports GKI2 (5.10+) kernels

---

## 🪝 Hook Modes

- **`Tracepoint Syscall Redirect hook`**: Default hook mode, from upstream, only supports GKI2 kernels with `arm64-v8a` or `x86_64` ABI
- **`Manual Hook`**: The most compatible hook, supports Linux kernels 3.4 to 6.18
- **`SUSFS Inline Hook`**: Hook from [SuSFS](https://github.com/simonpunk/susfs4ksu), similar to `Manual Hook`

---

## 🔧 Performance Optimization

### Kernel Hot Path Logs

v3.0.0 converted all `pr_info` calls in kernel high-frequency call paths to `pr_debug`, including:

- `syscall_hook.c` (arm64/x86_64/riscv64): 37 instances
- `ksud_integration.c`: 18 instances
- `setuid_hook.c`, `sucompat.c`, `dispatch.c`, `supercall.c`, etc.: 80+ instances

### sulog Fast Return

Added a fast return in `ksu_sulog_emit_grant_root`, saving subsequent logic for every `setresuid` when sulog is disabled.

---

## ⚠️ Upgrade Notice

### Package Name Change

- **Old Package Names**: `com.huijui.manager` / `com.huijuisu.huijuisu`
- **New Package Name**: `com.huijui.sunext`

Upgrading from the old version requires uninstalling it first before installing the new version.

### Post-upgrade Status

| Item | Status |
|---|---|
| Root Authorization List | ⚠️ Will be cleared, requires re-authorization |
| Module Enable Status | ✅ Preserved |
| SUSFS Configuration | ✅ Preserved |
| Backup History | ✅ Preserved |

---

## 📜 License

- Files in the `kernel/` directory are licensed under [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
- Art and visual files are created by [OukaroMF](https://github.com/OukaroMF) and licensed under a layered license. See [`ASSETS_LICENSE.md`](../ASSETS_LICENSE.md) for details.
- Except for the above files or directories, all other parts are licensed under [GPL-3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html).

---

## 🙏 Credits

- [KernelSU](https://github.com/tiann/KernelSU): Upstream foundation
- [BakaSU](https://github.com/Baka-SU/BakaSU): Base of this project
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra): Fork source
- [Magisk](https://github.com/topjohnwu/Magisk): Powerful Root tool
- [liquid-glass](https://github.com/nadeemiqbal/liquid-glass): Liquid glass UI library
- [genuine](https://github.com/brevent/genuine/): APK v2 signature verification

---

## 💬 Feedback

- GitHub Issues: https://github.com/hhcdxii-ship-it/HuijuiSU/issues
- QQ Group: Click "Join QQ Group" on the About page in the app.
