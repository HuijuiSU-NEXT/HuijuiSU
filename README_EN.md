# HuiJuiSU NEXT

> A deeply customized Android Root manager based on [BakaSU](https://github.com/Baka-SU/BakaSU)
> Preserves BakaSU's stable core while featuring extensive UI refactoring and functional enhancements tailored for the domestic modding environment.

[English](./README_EN.md) | [ç®€ä½“ä¸­æ–‡](./README.md)

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part Licenseï¼šGPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

---

## âœ¨ HuiJuiSU NEXT Core Features

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

## ðŸ§© Core Features Inherited from BakaSU

1. Kernel-based `su` and Root permission management
2. Module system based on [metamodules](https://kernelsu.org/guide/metamodule.html)
3. [App Profile](https://kernelsu.org/guide/app-profile.html): Lock Root privileges in a cage
4. Supports non-GKI and GKI 1.0
5. Supports multiple managers (Official KernelSU / KOWSU / BakaSU)

---

## âš™ï¸