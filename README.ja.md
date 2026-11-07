<div align="center">

# HuiJuiSU · Nova

**KernelSU 分支の Android マネージャー · 自前 Compose UI · 検出 / 隠蔽 / 自己修復を内蔵**

[![Release](https://img.shields.io/github/v/release/HuijuiSU-NEXT/HuijuiSU?label=release&color=2E9E6B)](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.13-4285F4)](https://developer.android.com/jetpack/compose)

[简体中文](README.md) · [繁體中文](README.zh-TW.md) · [English](README.en.md) · **日本語** · [한국어](README.ko.md) · [Русский](README.ru.md)

[ダウンロード](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) ·
[Telegram](https://t.me/Huijui123)

</div>

![ホーム](docs/images/v6.1/home.png)

---

## これは何か

**KernelSU フォーク用のマネージャー**です。ただし「動けばいい」で終わらせず、root 利用者が実際に踏む問題を内蔵機能にしました。

- **警告で終わらせず、直す**：チェックアップがモジュール競合を見つけ、自己修復が一括で無効化
- **コマンドをコピペさせない**：`resetprop` パッチ 12 種を内蔵、ワンタップで適用
- **嘘をつかない**：未対応のカーネル機能は「非対応」と表示し、押しても無反応なスイッチは出さない

UI は完全自前で、Compose の `foundation` だけに依存します。

---

## 他にはない 4 つの機能

| 機能 | ひとことで | 他にない理由 |
|---|---|---|
| **ワンタップチェックアップ** | 5 項目の検査 + 0〜100 点 + 修正への直リンク | 他は一覧だけで結論を出さない |
| **自己修復センター** | 競合モジュールを一括無効化（元に戻せる）+ キャッシュ削除 | 他は報告するだけで対処しない |
| **パッチセンター** | `resetprop` パッチ 12 種、再起動で自動復元 | 他はコマンドを書かせる |
| **4.x 互換モード** | 非 GKI カーネル向けの案内と boot パッチ手順 | 他は 4.x を切り捨てる |

---

## 画面

4 タブ構成、ダークガラス素材、動く壁紙。

| ホーム | Dock |
|---|---|
| ![ホーム](docs/images/v6.1/home.png) | ![Dock](docs/images/v6.1/dock.png) |

- ダークガラスのカード（上部 26% ハイライト、中央は壁紙が透ける、下端は締める）
- カード・Dock・ボタンで同じ素材言語
- 壁紙は**等比 cover**（引き伸ばしなし）。動画も静止画も同じ扱い
- 中国語 / 英語の切り替え（196 辞書、永続化）

---

## 機能

- **モジュール**：一覧 / 有効化 / 無効化 / アンインストール / オンライン導入 / ローカル zip
- **スーパーユーザー**：許可一覧、取り消し（カーネル ioctl）、パッケージ指定で追加、実アイコン表示
- **ツール**：書き込み / 検出 / ワンタップ隠蔽 / SUSFS / カーネル設定 / ターミナル
- **設定**：UI モード / ガラス / 明暗 / アクセント / 壁紙 / 言語 / 情報

---

## カーネル対応

| カーネル | 方式 |
|---|---|
| 6.x（6.6 / 6.12 / 6.18） | LKM |
| 5.10 / 5.15 | LKM |
| 4.x（4.9 / 4.14 / 4.19） | カーネルパッチ（**LKM 不可**） |
| 非 GKI 5.x | カーネルパッチ |

---

## インストール

1. [Releases](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) から ABI に合う APK を取得（迷ったら `universal`）
2. 上書きインストールで OK（`applicationId` は不変、データは保持）
3. 初回にモジュール画面で root を許可

---

## 他製品との比較

| | KernelSU | SukiSU | 本プロジェクト |
|---|---|---|---|
| SUSFS | ❌ | ✅ | ✅ |
| KPM | ❌ | ✅ | ❌（カーネル側未実装） |
| ワンタップチェックアップ | ❌ | ❌ | ✅ |
| 自己修復センター | ❌ | ❌ | ✅ |
| パッチセンター | ❌ | ❌ | ✅ |
| 4.x 互換モード | ❌ | ❌ | ✅ |
| 自前 UI | ❌ | ❌ | ✅ |

---

## 既知の制限

- **セーフモード**：起動時の音量キーで入るカーネル側の挙動で、書き込み API はありません
- **KPM**：このフォークのカーネル側にローダーがありません
- **多言語**：UI は中国語 / 英語のみ（README は 6 言語）
- **4.x**：案内とツールのみ。カーネルのビルドには端末のカーネルソースが必要

---

## リンク

- [更新履歴](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases)
- [サードパーティ通知](docs/THIRD_PARTY_NOTICES.md)
- [Telegram](https://t.me/Huijui123)

<div align="center">

**GPL-3.0** · カーネルモジュールとマネージャーは同一ライセンス

</div>
