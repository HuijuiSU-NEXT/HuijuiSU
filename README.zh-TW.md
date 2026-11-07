<div align="center">

# HuiJuiSU · Nova

**KernelSU 分支的 Android 管理器 · 自研 Compose 介面 · 內建偵測 / 隱藏 / 自癒**

[![Release](https://img.shields.io/github/v/release/HuijuiSU-NEXT/HuijuiSU?label=release&color=2E9E6B)](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.13-4285F4)](https://developer.android.com/jetpack/compose)

[简体中文](README.md) · **繁體中文** · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md)

[下載](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) ·
[Telegram](https://t.me/Huijui123) ·
[QQ 群 763278354](https://qm.qq.com/q/763278354)

</div>

![首頁](docs/images/v6.1/home.png)

---

## 這是什麼

一個 **KernelSU 分支的管理器**，但不只做到「能用」——它把 root 使用者真正會遇到的坑做成內建能力：

- **不只是警告，而是直接修好**：檢查找出模組衝突，自癒中心一鍵停用
- **不讓你上論壇抄指令**：12 條 `resetprop` 補丁內建，點一下就生效
- **不假裝支援**：核心沒有的特性會顯示「不支援」，不會給你點了沒反應的開關

介面完全自研，只依賴 Compose `foundation`，沒有任何第三方元件庫。

---

## 四項獨有能力

| 能力 | 一句話 | 為什麼別家沒有 |
|---|---|---|
| **一鍵檢查** | 5 項檢查 + 0–100 評分 + 直達修復入口 | 別家只給清單，不給結論 |
| **自癒中心** | 模組衝突一鍵停用（可逆）+ 清理快取 + 最近安裝風險提示 | 別家只回報，不處理 |
| **補丁中心** | 12 條 `resetprop` 補丁，重開機自動還原 | 別家要你自己抄指令 |
| **4.x 相容模式** | 非 GKI 核心的引導 + boot 修補工具鏈 | 別家直接不支援 4.x |

---

## 介面

四標籤管理器結構，深色玻璃材質，動態桌布背景。

| 首頁 | Dock |
|---|---|
| ![首頁](docs/images/v6.1/home.png) | ![Dock](docs/images/v6.1/dock.png) |

- **深色玻璃卡片**：頂部 26% 高光、中間透出桌布、底邊壓暗
- **統一材質語言**：卡片、Dock、按鈕同一套玻璃，沒有塑膠托盤感
- **桌布等比 cover**：影片與圖片都不拉伸
- **中 / 英切換**：196 條詞典、持久化

---

## 功能一覽

- **模組**：列表、啟停、卸載、線上安裝、本機 zip
- **超級使用者**：授權列表、撤銷（走核心 ioctl）、以套件名新增、真實圖示
- **工具**：刷寫、偵測、一鍵隱藏、SUSFS、核心設定、終端機
- **設定**：介面模式、玻璃、深淺色、強調色、桌布、語言、關於

---

## 核心支援

| 核心系列 | 支援方式 |
|---|---|
| 6.x（6.6 / 6.12 / 6.18） | LKM |
| 5.10 / 5.15 | LKM |
| 4.x（4.9 / 4.14 / 4.19） | 核心補丁（**不能用 LKM**） |
| 非 GKI 5.x | 核心補丁 |

---

## 安裝

1. 到 [Releases](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) 下載對應架構（不確定就選 `universal`）
2. 覆蓋安裝即可（`applicationId` 不變，資料保留）
3. 首次進入模組頁需授予 root

---

## 與其他專案比較

| | KernelSU | SukiSU | 本專案 |
|---|---|---|---|
| SUSFS | ❌ | ✅ | ✅ |
| KPM | ❌ | ✅ | ❌（核心未實作） |
| 一鍵檢查 | ❌ | ❌ | ✅ |
| 自癒中心 | ❌ | ❌ | ✅ |
| 補丁中心 | ❌ | ❌ | ✅ |
| 4.x 相容模式 | ❌ | ❌ | ✅ |
| 自研介面 | ❌ | ❌ | ✅ |

---

## 已知限制

- **安全模式**：開機音量鍵觸發，屬核心層行為，管理器沒有寫入介面
- **KPM**：本分支核心未實作載入器
- **多語言**：介面僅中 / 英（README 支援 6 種語言）
- **4.x**：提供引導與工具鏈，編譯核心仍需裝置核心原始碼

---

## 連結

- [更新日誌](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
- [第三方元件清單](docs/THIRD_PARTY_NOTICES.md)
- [Telegram](https://t.me/Huijui123) · [QQ 群](https://qm.qq.com/q/763278354)

<div align="center">

**GPL-3.0** · 核心模組與管理器同源

</div>
