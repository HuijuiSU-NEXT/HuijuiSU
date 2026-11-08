<div align="center">

# HuiJuiSU · Nova

**An Android manager for a KernelSU fork · Hand-built Compose UI · Built-in detect / hide / self-heal**

[![Release](https://img.shields.io/github/v/release/HuijuiSU-NEXT/HuijuiSU?label=release&color=2E9E6B)](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.13-4285F4)](https://developer.android.com/jetpack/compose)
[![MinSDK](https://img.shields.io/badge/Android-8.0%2B-3DDC84)](https://developer.android.com)

**English** · [简体中文](README.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md)

**Latest: v6.1.5** — ships 16 `kernelsu.ko` builds (8 KMI × 2 ABIs)

[Download](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) ·
[**Install routes (read first)**](docs/INSTALL_ROUTES.md) ·
[Changelog](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) ·
[Telegram](https://t.me/Huijui123)

</div>

![Home](docs/images/v6.1/home.png)

---

## What it is

A **manager for a KernelSU fork** — but it does more than "work". It turns the problems root users actually hit into built-in capabilities:

- **It doesn't just warn you — it fixes it**: the checkup finds module conflicts, the self-heal screen disables them in one tap
- **No copying commands from forums**: 12 built-in `resetprop` patches, one tap to apply
- **No fake toggles**: unsupported kernel features say "unsupported" instead of pretending

The UI is fully hand-built on Compose `foundation` — no third-party component library.

---

## Four features you won't find elsewhere

| Feature | In one line | Why others don't have it |
|---|---|---|
| **One-tap checkup** | 5 checks + 0–100 score + direct fix links | Others give lists, not conclusions |
| **Self-heal center** | Disable conflicting modules (reversible) + cache cleanup + recent installs | Others report, but don't act |
| **Patch center** | 12 `resetprop` patches, auto-reverted on reboot | Others make you type commands |
| **4.x compat mode** | Guidance + boot patching for non-GKI kernels | Others simply don't support 4.x |

---

## UI

Four-tab manager shell, dark glass material, dynamic wallpaper.

| Home | Dock |
|---|---|
| ![Home](docs/images/v6.1/home.png) | ![Dock](docs/images/v6.1/dock.png) |

- **Dark glass cards**: 26% top highlight, wallpaper showing through the middle, darkened bottom edge
- **One material language**: cards, dock and buttons share the same glass — no plastic tray feel
- **Typography**: 52sp clock with negative tracking, two-level grey scale, per-level letter spacing
- **Dynamic wallpaper**: video/image fills the screen, cards float above, never lost when switching pages
- **Chinese / English switch**: 196 dictionary entries, persisted

---

## Features

<details open>
<summary><b>Manager</b></summary>

- Four tabs: Home / Modules / Superuser / Settings
- Modules: list, enable/disable, uninstall, install from repo, install local zip
- Superuser: grant list, revoke (kernel ioctl, falls back to editing the allowlist), add by package
- Settings: UI mode, glass material, light/dark, accent, wallpaper, language, kernel settings, about

</details>

<details>
<summary><b>Tools</b></summary>

- **Flash center**: install module / patch boot / flash image (with double confirmation)
- **Detection**: 9 groups of fingerprint checks, highlights exposed items
- **One-tap hide**: built-in hide-bundle suite
- **SUSFS panel**: master switch / logging / AVC spoofing / hidden paths / config backup
- **Kernel settings**: su compat / kernel umount / sulog / adb root / SELinux hide
- **Terminal**: temporary root shell
- **Online module repo**: 7 mirrors with automatic fallback

</details>

<details>
<summary><b>Unique</b></summary>

- One-tap checkup · Self-heal center · Patch center · 4.x compat mode

</details>

---

## Kernel support

| KMI | Android | LKM | Method |
|---|---|---|---|
| `android12-5.10` | 12 | ✅ | LKM |
| `android13-5.10` | 13 | ✅ | LKM |
| `android13-5.15` | 13 | ✅ | LKM |
| `android14-5.15` | 14 | ✅ | LKM |
| `android14-6.1` | 14 | ✅ | LKM |
| `android15-6.6` | 15 | ✅ | LKM |
| `android16-6.12` | 16 | ✅ | LKM |
| `android17-6.18` | 17 | ✅ | LKM |
| 4.x (4.9 / 4.14 / 4.19) | 8–11 | ❌ | Kernel patch |
| Non-GKI 5.x | 10–13 | ❌ | Kernel patch |

All `kernelsu.ko` files ship with each release, prefixed by ABI:

```
aarch64-android16-6.12_kernelsu.ko
x86_64-android16-6.12_kernelsu.ko
```

The manager detects the kernel form automatically — not working / LKM / GKI built-in / 4.x non-GKI — and shows it on the home tab.

---

## Install

> **Check your kernel type first.** Picking the wrong path is the #1 reason for "manager installed but no root".

| Your kernel | Path |
|---|---|
| GKI 2.0 (5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18) | **Path A: LKM** (no kernel rebuild) |
| 4.x or non-GKI custom kernel | **Path B: kernel patch** (rebuild boot) |

Full steps, commands and troubleshooting 👉 **[Install routes](docs/INSTALL_ROUTES.md)**

### Path A — LKM (GKI 2.0)

```bash
# 1. Grab the ko matching your KMI, e.g. 6.12 on arm64
adb push aarch64-android16-6.12_kernelsu.ko /data/local/tmp/
adb shell su -c "ksud insmod /data/local/tmp/aarch64-android16-6.12_kernelsu.ko"
adb shell su -c "lsmod | grep -i ksu"

# 2. Install the manager APK
```

### Path B — kernel patch (4.x / non-GKI)

Fetch the device kernel source → apply the KernelSU patch → build boot → `fastboot flash boot`.
Full commands: [install routes](docs/INSTALL_ROUTES.md).

### Both

1. Download the right ABI from [Releases](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
   - Not sure? Use `universal`
2. Install over the old version — `applicationId` is stable, data is kept
3. Grant root on first visit to the Modules tab

| ABI | Target |
|---|---|
| `arm64-v8a` | Most modern phones |
| `armeabi-v7a` | Older 32-bit devices |
| `x86_64` | Emulators / tablets |
| `riscv64` | RISC-V |
| `universal` | When in doubt |

---

## Build from source

```bash
# Manager (JDK 21 + Android SDK 37)
cd manager
./gradlew :app:assembleRelease

# Kernel module and ksud
cd userspace/ksud
cargo build --release --target aarch64-linux-android
```

CI builds 16 LKM targets plus ksud for 4 architectures.

---

## How it compares

| | KernelSU | SukiSU | This project |
|---|---|---|---|
| Module management | ✅ | ✅ | ✅ |
| Superuser | ✅ | ✅ | ✅ |
| SUSFS | ❌ | ✅ | ✅ |
| Kernel feature toggles | ✅ | ✅ | ✅ |
| KPM | ❌ | ✅ | ❌ (not in this kernel) |
| **One-tap checkup** | ❌ | ❌ | ✅ |
| **Self-heal center** | ❌ | ❌ | ✅ |
| **Patch center** | ❌ | ❌ | ✅ |
| **4.x compat mode** | ❌ | ❌ | ✅ |
| **Hand-built UI** | ❌ | ❌ | ✅ |

---

## Known limitations

- **Safe mode**: triggered by volume key at boot — a kernel-level behaviour with no write interface for the manager
- **KPM**: the kernel side of this fork has no KPM loader
- **Localization**: covers the shell and common items; long descriptions remain in Chinese
- **4.x**: guidance and tooling only — building a kernel still needs device kernel sources

---

## Links

- [Changelog](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
- [Third-party notices](docs/THIRD_PARTY_NOTICES.md)
- [On-device verification checklist](docs/VERIFY.md)
- [Telegram](https://t.me/Huijui123)

---

<div align="center">

**GPL-3.0** · Kernel module and manager share the same license

</div>
