# HuiJuiSU NEXT

> [BakaSU](https://github.com/Baka-SU/BakaSU) をベースに深くカスタマイズされた Android Root マネージャー
> BakaSU の安定したコアを維持しつつ、国内の modding 環境に合わせて UI を大幅に再構築し、機能を強化しました。

[English](./README_EN.md) | [简体中文](./README.md) | [日本語](./README_JA.md) | [한국어](./README_KO.md) | [Русский](./README_RU.md)

[![Latest release](https://img.shields.io/github/v/release/hhcdxii-ship-it/HuijuiSU?label=Release&logo=github)](https://github.com/hhcdxii-ship-it/HuijuiSU/releases/latest)
[![Kernel License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Other part License：GPL v3](https://img.shields.io/github/license/hhcdxii-ship-it/HuijuiSU?logo=gnu)](../LICENSE)

---

## ✨ HuiJuiSU NEXT の主な機能

### 独立したツールハブ

下部ナビゲーションが **5 つのタブ**に再設計されました：ホーム / **ツール** / スーパーユーザー / モジュール / 設定。

ツールはホームページから分離され、情報密度が高くなりすぎるのを防ぎ、見つけやすくなりました。

### 7 つの強力なツール

| 機能 | 説明 |
|---|---|
| **ワンタップヘルスチェック** | バッテリー / 温度 / BL / カーネル / Root / SELinux / ストレージ |
| **パフォーマンスモード** | 省電力 / バランス / パフォーマンス / ゲーム、4 段階のシステムスケジューリング |
| **ワンタップバックアップパッケージ** | boot + init_boot + vbmeta + モジュールスナップショット、restore.bat 付き |
| **ワンタップアンブリック** | バックアップディレクトリからワンクリックで dd フラッシュバック、モジュール状態の復元も可能 |
| **GKI カーネルダウンロード** | built-in モード専用のコミュニティプリコンパイルカーネルへのリダイレクト |
| **カスタムカーネルのフラッシュ** | `.img` (built-in) と `.ko` (LKM) の両形式をサポート |
| **アプリマネージャー** | 凍結 / 解凍 / 強制停止 / データ消去 / アンインストール（現在のユーザー） |

### 内蔵の隠蔽（デフォルトで最強）

HuiJuiSU NEXT は**すぐに使える**隠蔽戦略を採用しています：

- **パス隠蔽**：8 つの重要な `sus_path`（ksu / ksud / modules / magisk / tmp など）
- **マウント隠蔽**：`hide_sus_mnts_for_non_su_procs` が sus マウントポイントを隠蔽
- **ログ偽装**：`avc_log_spoofing` が SELinux 拒否ログを隠蔽
- **カーネル統計隠蔽**：`sus_kstat` が `/system` と `/vendor` を上書き
- **自動適用**：アプリ起動時にバックグラウンドでサイレント実行され、ルール更新後に自動再適用

### モジュールスナップショット

バックアップ時に `/data/adb/modules/` 下の全モジュールの有効/無効状態を `modules.json` に自動記録します。

アンブリック時に「モジュール状態も復元」にチェックを入れると、バックアップ時の構成にモジュールを自動復元します。**手動で一つずつ有効にする必要はありません**。

### カスタムカーネルのフラッシュ

GKI デバイスの built-in モードの課題を解決：

- **`.img` 形式**：Root 権限で dd フラッシュ + vbmeta 検証処理（ロールバック防止）+ LKM 残存のクリア（オプション）
- **`.ko` 形式**：`ksud boot-patch -f -m` を呼び出して自動注入
- **スマート認識**：ファイルタイプとサイズを自動検出し、誤ったファイルのフラッシュを防止

---

## 🧩 BakaSU から継承されたコア機能

1. カーネルベースの `su` と Root 権限管理
2. [metamodules](https://kernelsu.org/guide/metamodule.html) に基づくモジュールシステム
3. [App Profile](https://kernelsu.org/guide/app-profile.html)：Root 権限を檻の中に閉じ込める
4. 非 GKI および GKI 1.0 をサポート
5. 複数のマネージャーをサポート（公式 KernelSU / KOWSU / BakaSU）

---

## ⚙️ 互換性ステータス

- Android **GKI 2.0** デバイス（カーネル 5.10+）を公式サポート
- 古いカーネル（3.4+）も互換性がありますが、カーネルを自分でコンパイルする必要があります
- サポートアーキテクチャ：`arm64-v8a`、`armeabi-v7a`、`x86_64`
- 内蔵の [SUSFS](https://gitlab.com/simonpunk/susfs4ksu) はカーネル 4.3+ へのバックポート**のみをサポート**
- `Tracepoint Syscall Redirect hook` は GKI2（5.10+）カーネルのみをサポート

---

## 🪝 フックモード

- **`Tracepoint Syscall Redirect hook`**：デフォルトのフックモード、アップストリームから、`arm64-v8a` または `x86_64` ABI の GKI2 カーネルのみをサポート
- **`Manual Hook`**：最も互換性の高いフック、Linux カーネル 3.4 から 6.18 をサポート
- **`SUSFS Inline Hook`**：[SuSFS](https://github.com/simonpunk/susfs4ksu) からのフック、`Manual Hook` と類似

---

## 🔧 パフォーマンス最適化

### カーネルホットパスログ

v3.0.0 でカーネルの高頻度呼び出しパス内の `pr_info` をすべて `pr_debug` に変換しました：

- `syscall_hook.c`（arm64/x86_64/riscv64）：37 箇所
- `ksud_integration.c`：18 箇所
- `setuid_hook.c`、`sucompat.c`、`dispatch.c`、`supercall.c` など：80 箇所以上

### sulog 高速リターン

`ksu_sulog_emit_grant_root` に高速リターンを追加し、sulog 無効時に毎回の `setresuid` で後続のロジックを省略します。

---

## ⚠️ アップグレード通知

### パッケージ名の変更

- **旧パッケージ名**：`com.huijui.manager` / `com.huijuisu.huijuisu`
- **新パッケージ名**：`com.huijui.sunext`

旧バージョンからのアップグレードは、旧バージョンをアンインストールしてから新バージョンをインストールする必要があります。

### アップグレード後の状態

| 項目 | 状態 |
|---|---|
| Root 認証リスト | ⚠️ 消去され、再認証が必要 |
| モジュール有効状態 | ✅ 保持 |
| SUSFS 設定 | ✅ 保持 |
| バックアップ履歴 | ✅ 保持 |

---

## 📜 ライセンス

- `kernel/` ディレクトリ内のファイルは [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) の下でライセンスされています
- アートおよびビジュアルファイルは [OukaroMF](https://github.com/OukaroMF) によって作成され、レイヤードライセンスの下で提供されています。詳細は [`ASSETS_LICENSE.md`](../ASSETS_LICENSE.md) をご覧ください。
- 上記のファイルまたはディレクトリを除き、その他のすべての部分は [GPL-3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html) の下でライセンスされています。

---

## 🙏 クレジット

- [KernelSU](https://github.com/tiann/KernelSU)：アップストリーム基盤
- [BakaSU](https://github.com/Baka-SU/BakaSU)：本プロジェクトのベース
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)：Fork 元
- [Magisk](https://github.com/topjohnwu/Magisk)：強力な Root ツール
- [liquid-glass](https://github.com/nadeemiqbal/liquid-glass)：リキッドグラス UI ライブラリ
- [genuine](https://github.com/brevent/genuine/)：APK v2 署名検証

---

## 💬 フィードバック

- GitHub Issues: https://github.com/hhcdxii-ship-it/HuijuiSU/issues
- QQ グループ：アプリ内の About ページで「Join QQ Group」をクリック
