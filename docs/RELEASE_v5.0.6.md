# HuiJuiSU NEXT Ultra v5.0.6

> 内置隐藏模块升级为 **v1.4 超级定制版**：去掉不可用入口、砍掉 120 MB 冗余、修好已知配置错误。

| | |
|---|---|
| 版本号 | `v5.0.6 (35344)` |
| 基线 | v5.0.5 |
| 包名 | `com.huijui.sunext.ultra`（覆盖安装） |
| APK | 5 个架构，38.2 – 50.0 MB |
| 内置模块 | `hide_bundle` **v1.4-super**（26 MB） |

---

## 一、内置模块升级到 v1.4

模块本体从作者的 v1.3 升级到 v1.4，并做成**随管理器分发的超级定制版**。

### 移除

| 项 | 原因 |
|---|---|
| **「安装检测软件（7）」按钮** | 检测 APK 已不进核心，入口留着必定失败 |
| `detect/` 下 6 个检测 APK（约 120 MB） | 属于作者自测工具，不该塞进管理器 |
| 对应的 WebUI 事件处理 | 按钮删了，JS 分支一并清掉 |

### 保留（全部可用）

| 功能 | 说明 |
|---|---|
| 补装 / 应用设置 | `oneclick.sh` |
| 重扫名单 / 刷新状态 / 查看日志 | 原有入口 |
| 替换 keybox | 从 Download 选文件 |
| **导入 HMA 配置** | 保留，仍支持外部 json |
| 用包内自带配置 | 自动导入 `hma/config.json` |
| 重新导入模块配置 | `oneclick.sh --reimport` |
| 关闭无线调试 | 原有脚本 |
| 春秋修复工具 | 原有脚本 |

---

## 二、修掉的已知配置错误

| # | 问题 | 修复 |
|---|---|---|
| 1 | `chunqiu_fix.sh` 找的是 `JinBeiChunQiuFixToolPro2.sh`，实际文件叫 `jinbei_chunqiu_fix.sh` | 路径改为实际文件名 |
| 2 | `customize.sh` 兜底解压列表没有 `scripts/*`，部分管理器解压不全时脚本会缺 | 补齐 `scripts/*` |
| 3 | `oneclick.sh` 横幅写「v2.0」，与模块版本不符 | 改为 `v1.4 · 超级定制版` |
| 4 | 部署时旧版本包不清理，`/data/adb/huijui` 会越堆越多 | 部署后自动删除旧 `hide_bundle_v1*.zip` |
| 5 | 体积 165 MB，其中 120 MB 是检测工具 | 定制版压到 **26 MB** |

---

## 三、模块信息

```
id=hide_bundle
name=一键隐藏（78 整合 · 恋雪）超级定制版
version=v1.4-super
versionCode=112
author=恋雪 / HuiJuiSU
```

内置内容：

| 部分 | 说明 |
|---|---|
| `payload/` | Zygisk Next 1.5.2 / LSPosed / HMA-OSS UID3.1 / OhMyKeymint / PIF-Dates |
| `apk/hma-oss.apk` | HMA 管理端 |
| `hma/config.json` | 内置 HMA 配置 |
| `keybox/keybox.xml` | 内置 keybox |
| `scripts/` | 春秋修复 / 关无线调试 / 导入配置 / 换 keybox |
| `webroot/` | WebUI（含背景图） |

---

## 四、管理器侧配套

| 项 | 变更 |
|---|---|
| 内置资源 | `hide_bundle_v1.4_super.zip` |
| 部署版本号 | `VERSION = 6`，已部署过的设备会自动覆盖更新 |
| 开机自启模块 | `huijui_core/boot-completed.sh` 引用同步 |
| 旧包清理 | 部署时自动移除历史 `hide_bundle_v1*.zip` |

---

## 五、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.7 MB |
| `armeabi-v7a` | 老 32 位设备 | 38.2 MB |
| `x86_64` | 模拟器 / 平板 | 40.2 MB |
| `riscv64` | RISC-V | 38.6 MB |
| `universal` | 不确定选这个 | 50.0 MB |

覆盖安装即可，数据保留。

---

## 六、验证方法

装好后检查三件事：

1. 关于页显示 `v5.0.6`
2. 模块 WebUI 里「安装检测软件」按钮已消失，「导入 HMA 配置」仍在
3. 工具箱 → 一键隐藏，装出的模块版本为 `v1.4-super`

---

## 七、限制（未变）

| 项 | 说明 |
|---|---|
| 检测 APK | 定制版不再内置，需要的话从作者原包取 |
| 热刷入 | 仅纯 VFS 文件计划的模块可免重启 |
| 开机激活 | 需要先打开一次管理器完成资源落盘 |

---

<sub>完整对比：`git log v5.0.5..v5.0.6`</sub>
