# 第三方组件与许可（Third-Party Notices）

本项目以 **GPL-3.0** 发布（见仓库根目录 [LICENSE](../LICENSE)）。下面是随 APK 一起分发、
或在本项目开发中直接使用的第三方组件及其许可。

## 运行时随 APK 分发

| 组件 | 版本 | 许可 | 用途 | 链接 |
|---|---|---|---|---|
| **Miuix**（miuix-ui / icons / squircle / nav / blur） | 0.9.4 | Apache-2.0 | HyperOS 风格 Compose 组件（Surface/Text/Button/NavigationBar 等） | https://github.com/compose-miuix-ui/miuix |
| **Jetpack Compose**（ui / foundation / runtime / animation） | 1.13.x | Apache-2.0 | 全部界面渲染 | https://developer.android.com/jetpack/compose |
| **AndroidX**（activity-compose / core / lifecycle 等） | 各自版本 | Apache-2.0 | 基础 Android 能力 | https://developer.android.com/jetpack/androidx |
| **Kotlin 标准库 / kotlinx.coroutines** | 2.4.x / 1.10.x | Apache-2.0 | 语言运行时与协程 | https://kotlinlang.org |
| **libsu** | 上游版本 | Apache-2.0 | root shell 交互（经典界面） | https://github.com/topjohnwu/libsu |

## 内置资源

| 资源 | 许可 | 说明 |
|---|---|---|
| `assets/nova/miku_star.mp4` | 由项目维护者提供 | 内置动态壁纸，随 APK 分发 |
| `assets/detect/chunqiu_check.sh` | 由项目维护者提供 | 春秋检测脚本（等效复刻，不含原 APK 代码） |
| `assets/hide/hide-bundle-v2.0.zip` | 由模块作者提供 | 一键隐藏套件（内含 Zygisk Next / LSPosed / HMA-OSS / OhMyKeymint，各自遵循其原许可，**仅限自用，禁止二次分发**） |
| 图标 | 本项目手写 | 24×24 path，无第三方图标库 |

## 上游项目

| 项目 | 许可 | 关系 |
|---|---|---|
| [KernelSU](https://github.com/tiann/KernelSU) | GPL-3.0 | 内核模块与 ksud 的原始实现 |
| [BakaSU](https://github.com/Baka-SU/BakaSU) | GPL-3.0 | 本仓库的直接上游 |

## 注意

- `hide-bundle` 内含的 Zygisk Next、OhMyKeymint 等组件有各自的授权限制，**本仓库不为其再分发授权背书**；
  内置仅为了安装流程完整，使用者需自行确认合规。
- 若你是某个组件的权利人并认为本仓库的使用不当，请开 issue 或联系维护者，我们会调整。

---

最后更新：2026-11-07
