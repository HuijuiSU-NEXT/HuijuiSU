# HuiJuiSU v6.0 发布状态（截至 2026-11-06 23:15）

## 已完成

- 代码：WSL `~/HuijuiSU`，分支 `feature/huijui-ultra`，已 push
  - `362a4cd2a` 主体功能
  - `49d2148a2` 修复 release dex 重复 + 版本号 v6.0
- tag `v6.0` 已推送 → 触发 `Release` 流水线（2026-11-06 14:50 UTC 入队）
- 本地 release 构建**已通过**：59 MB universal（关键修复：`gradle/libs.versions.toml` 里
  `miuix-nav` 必须用 `miuix-nav-android`，否则 D8 报 `PagerDragEvent is defined multiple times`）

## 进行中

- GitHub Actions：
  - `Release` (v6.0) — in_progress
  - `Build Manager` (v6.0) — in_progress
  - 旧提交 `362a4cd` 的 Build Manager — failure（修复前的构建，属预期）

## 待办

1. 等 Release run 出结论
2. 成功 → 核对 https://github.com/HuijuiSU-NEXT/HuijuiSU/releases/tag/v6.0 的 APK 资产
   与 TG 频道 https://t.me/Huijui123 的通知
   （workflow 的 `CHAT_ID` 已兜底 `@Huijui123`；需要仓库 secret `BOT_TOKEN` 存在）
3. 失败 → 读日志修复后重打 tag（先删远端 tag）或 bump v6.0.1
4. 发布成功后关机

## 环境备忘

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_NDK_HOME=$HOME/android-sdk/ndk/30.0.16248370
cd ~/HuijuiSU/manager
./gradlew :app:assembleRelease
```

- 模拟器：`nova_px`（`adb -s emulator-5554`），启动命令带 `-gpu swiftshader_indirect`
- 单测：45 个，全绿
