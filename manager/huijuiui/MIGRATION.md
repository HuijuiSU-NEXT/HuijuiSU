# 从 Material3 旧界面迁移到 Nova

旧界面（`org.bakasu.bakasu.ui.*`）是按 BakaSU 的 Material3 语义搭的：`Scaffold` +
`TopAppBar` + `NavigationBar` + `Card` + `ListItem`。Nova 刻意不提供这些，所以迁移不是
「换主题」，而是**按屏幕换骨架**。下面是一份可以照做的对照表。

## 骨架替换

| 旧 | 新 |
| --- | --- |
| `Scaffold` + `TopAppBar` | `Box` + `NovaTopBar` + `Column` |
| `NavigationBar` / `FloatingBottomBar` | `NovaDock`（自绘，`HuiSurface` 玻璃） |
| `NavHost` 页面切换 | `Crossfade` + `HuiEase.Emphasized`（或 `huiEnter` 指定变体） |
| `Card` / `ElevatedCard` | `HuiCard`（材质由 `LocalHuiMaterial` 决定） |
| `ListItem` | `HuiListRow` |
| `Switch` | `HuiToggle` / `HuiToggleRow` |
| `Button` / `TextButton` | `HuiButton(style = Primary/Glass/Ghost/Danger)` |
| `SegmentedButton` | `HuiSegmented`（3~4 项）或 `HuiChipRow`（多到放不下） |
| `LinearProgressIndicator` | `HuiProgressBar` / `HuiProgressCard` |
| `CircularProgressIndicator` | `HuiRingGauge` |
| `Icon(Icons.Default.*)` | `HuiIcon(HuiIcons.*)`（36 个手写路径） |
| `MaterialTheme.typography.*` | `LocalHuiTypography.current.{display,headline,title,subtitle,body,bodySmall,label,micro,numeric}` |
| `MaterialTheme.colorScheme.*` | `LocalHuiColors.current.{accent,text,textDim,textFaint,danger,...}` |
| `MaterialTheme.shapes.*` | `LocalHuiShapes.current.{hero,card,tile,pill,sheet,tiny}` |

## 屏幕对照

| 旧文件 | Nova 对应 | 备注 |
| --- | --- | --- |
| `ui/screen/main/HomePage` | `NovaHomeScreen` | Hero + 状态卡 + 快捷磁贴 + 模块卡 |
| `ui/screen/settings/*`、`themeSettings` | `NovaSettingsScreen` | 材质/强调色/动效三组，`SettingsBaseWidget` 系列可整体废弃 |
| `ui/screen/moduleRepo/*` | 待做 | 用 `HuiModuleCard` + `HuiChipRow` 过滤 + `HuiProgressCard` 下载进度 |
| `ui/screen/susfs/*` | 待做 | 子页面用 `HuiBannerCard` + `HuiListRow` |
| `ui/screen/Flash.kt` / `kernelFlash` | `NovaToolsScreen` 的刷写区 | 危险动作用 `HuiTone.Danger` |
| `ui/screen/AppProfile.kt` | 待做 | 权限表用 `HuiToggleRow` 列表 |
| `ui/screen/SulogScreen.kt` | 待做 | `HuiCard` + 等宽文本，建议虚拟列表 |
| `ui/screen/DynamicManagerScreen.kt`、`UmountManagerScreen.kt` | 待做 | `HuiListRow` + `HuiBadge` |
| `ui/screen/Install.kt` | 待做 | 进度用 `HuiProgressCard`，步骤用 `HuiMotionCatalog` 逐条入场 |
| `ui/webui/*` | 不变 | WebUI 是独立 Activity，跟 UI 语言无关 |

## 迁移顺序建议

1. **设置页**先迁：纯声明式、没有 Root 调用，风险最低，且能立刻暴露材质/字阶问题。
2. **首页**再迁：信息密度最高，验证完 Hero/Stat/磁贴三种卡片的组合效果。
3. **模块仓库**：引入列表 + 下载进度，验证长列表下的玻璃性能。
4. **刷机/安装**最后：涉及 `Shell` 与失败回滚，逻辑复用旧 `viewmodel`，只换渲染层。

每一步都可以用 `HuiBackdropHost` 包住整屏，旧的 `WebUIActivity` 之前不需要动。

## 边界

- `huijui/feature/*` 里那批业务（体检、备份、救援）目前是 Composable + `Shell` 直调；
  迁移时**只换它们内部的 UI 部分**，`HuijuiShellUtils` 那层命令逻辑原样保留。
- 迁移期间两套界面共存是安全的：Nova 在独立 Activity 里，不共享 `NavHost`、
  不共享 `MaterialTheme`，唯一的交集是 `huijui/feature` 的业务函数。
