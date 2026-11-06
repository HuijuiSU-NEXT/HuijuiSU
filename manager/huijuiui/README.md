# HuiJuiSU Nova UI

一套从零写起的界面层，用来替换管理器里那套继承自 BakaSU 的 Material 3 界面。
**不是换皮**：这里没有 `MaterialTheme`、没有 `Card`/`Button`/`Switch`/`NavigationBar`，
连图标都是 24×24 手写路径。整个模块只依赖 `compose-ui` + `foundation`。

```
manager/huijuiui/
└── src/main/kotlin/com/huijui/nova/
    ├── design/       设计令牌：颜色、字阶、形状、间距、三种材质定义
    ├── material/     材质渲染：极光背景、可被采样的 GraphicsLayer、AGSL 液态玻璃
    ├── motion/       运动引擎：8×8×16 = 1024 个动画变体 + 按压/呼吸/自转
    ├── components/   自绘图标集、文本、按钮、开关、分段器、进度、卡片族
    └── screens/      首页、工具、实验台（材质/动效）、设置
```

## 三种材质

| 材质 | 实现 | 适用 |
| --- | --- | --- |
| 柔光玻璃 `SoftGlass` | 背景 GraphicsLayer 采样 + `BlurEffect(26dp)` + 白色薄涂 | 默认，层次最明显 |
| 液态玻璃 `LiquidGlass` | 模糊之上再串一个 AGSL `RuntimeShader`：边缘折射位移、RGB 色散、流动高光带、边缘反光 | 想要「会呼吸的玻璃」 |
| 纯色 `Solid` | 零模糊，双层竖向渐变 + 1px 描边 | 省电、户外、低端机 |

实现要点：`HuiBackdropHost` 把极光背景 `record` 进一个 `GraphicsLayer`，
卡片再用 `translate(-origin)` + `drawLayer(backdrop)` 取「自己背后那一块」。
所以玻璃看到的是真实背景，而不是贴一层半透明白。

液态玻璃的着色器在 `HuiSurface.kt` 的 `LIQUID_GLASS_AGSL`，
通过 `RenderEffect.createChainEffect(shader, blur)` 串在模糊之后，
`uTime` 每帧在 `graphicsLayer {}` 里更新，不触发重组。API < 33 自动退化为柔光玻璃。

## 动画

`HuiMotionCatalog` 由三个维度组合出 **1024 个可寻址变体**：

- 8 个入场族：淡入 / 上浮 / 下沉 / 左滑 / 右滑 / 缩放 / 旋转 / 翻转
- 8 条缓动曲线：线性 / 标准 / 强调 / 减速 / 加速 / 过冲 / 预备 / 弹性
- 16 档时长：110ms → 935ms，步进 55ms

每个变体的位移、起始缩放、旋转角由 id 决定（`(id * 37) % 17`），
所以 1024 个 id 的实际姿态远多于 1024 种。调用方式：

```kotlin
Modifier.huiEnter(HuiMotionCatalog.forKey("hero"))      // 同一张卡片每次入场一致
Modifier.huiEnter(HuiMotionCatalog.get(742), delayMs = 60)
```

装饰性动画：`huiPress`（按压回弹）、`huiBreathe`（呼吸）、`huiSpin`（光环自转）、
`huiWave`（列表错峰起伏）。动效强度 `Calm/Flow/Wild` 会改时长倍率并关闭装饰动画。

## 组件

`HuiCard`（基类）、`HuiHeroCard`、`HuiStatCard`、`HuiActionTile`、`HuiListRow`、
`HuiModuleCard`、`HuiProgressCard`、`HuiBannerCard`、`HuiToggleRow`、
`HuiRingGauge`、`HuiSparkline`、`HuiButton`、`HuiToggle`、`HuiSegmented`、
`HuiChipRow`、`HuiBadge`、`HuiSectionTitle`、`HuiEmptyState`。

卡片的形状语言是刻意不对称的：Hero 用 `36/36/36/14`，磁贴用 `24/10/24/24`，
Dock 用整圆。没有 Material 的 tonal elevation —— 层次来自材质、描边和阴影。

## 入口

`org.bakasu.bakasu.nova.NovaActivity`，在 Manifest 里以 `activity-alias`
注册成独立图标「HuiJuiSU Nova」，与主界面并存、互不影响。

```bash
./gradlew :app:assembleDebug
adb shell am start -n com.huijui.sunext/org.bakasu.bakasu.nova.NovaLauncher
```

## 动态壁纸

界面的背景本身就是一块壁纸画布，五种预设全部程序化绘制（没有视频解码）：

| 预设 | 绘制方式 | 帧率预算 |
| --- | --- | --- |
| 极光 Aurora | 三团径向渐变 + 缓慢漂移 | 4fps |
| 流体渐变 Mesh | 四角渐变叠加，周期错开 | 8fps |
| 流线 Flow | 46 条正弦流线 + 上下收边 | 12fps |
| 星尘 Particle | 90 个哈希定位粒子带视差上升 | 10fps |
| 纯色 Solid | 单色底，零动画 | 0fps |

设置在「设置 → 动态壁纸」里，缩略图用的是同一套绘制代码的静态相位，所见即所得。
玻璃卡片采样的是同一块画布，换壁纸不需要改任何卡片。

### 系统级动态壁纸

`org.bakasu.bakasu.nova.wallpaper.NovaWallpaperService` 是一个真正的 `WallpaperService`，
用 `android.graphics` 重画同一套极光配色，可以直接设为桌面壁纸（长按桌面 → 壁纸 →
HuiJuiSU Nova）。它同样在 `:nova` 进程里，并且：

- 只有 `onVisibilityChanged(true)` 才启动帧循环，桌面不可见立刻停；
- 帧率锁 30fps，极光是慢速运动，足够；
- 每帧只重绘渐变，不做位图分配。

### 默认壁纸：初音配色

首屏默认就是 `Miku` 预设：青绿 #39C5BB + 洋红 #E12885，两道左右镜像的「双马尾」流光带、
稀疏光点，12fps。它**不是**导入的图片或视频，而是纯代码画的渐变与曲线——所以没有版权
问题，也不占安装包体积。

想换成真正的初音插画或视频：设置 → 本地媒体 → 选你自己的 MP4/GIF，几秒钟的事。

#### 内置视频壁纸

`app/src/main/assets/nova/miku_star.mp4` 是随包提供的动态壁纸（用户素材，900x1600 30fps，
5.9MB，去音轨）。首次进入 Nova 时在 IO 线程解到 `filesDir` 并设为默认，之后复用，
用户手动清除后不会再自动恢复（`media_initialized` 标记）。

视频壁纸下玻璃卡片会退化成深色遮罩（42% 底色调），而不是薄白——否则压在视频上会发灰。

### 本地媒体壁纸（图片 / GIF / MP4）

「设置 → 本地媒体 → 选择文件」调用系统文件选择器，取持久化读权限后写入
SharedPreferences（跨进程，壁纸服务也能读到）。

| 类型 | 渲染通道 | 玻璃采样 |
| --- | --- | --- |
| PNG / JPG / WebP | ImageDecoder 画进 Compose 画布 | 正常 |
| GIF | AnimatedImageDrawable，靠自身回调驱动重绘 | 正常 |
| MP4 / WebM | TextureView + VideoView 循环静音播放 | 退化为纯色遮罩 |

视频为什么退化：TextureView 是系统合成层，不在 Compose 的绘制树里，
`GraphicsLayer` 录不到它，所以视频壁纸下玻璃卡片自动走「无背景图层」分支
（`HuiSurface` 本来就有这条兜底），不会出现采样到黑块的 bug。

系统级壁纸服务另外维护三条通道：程序化极光、图片/GIF 逐帧、MP4 交给 MediaPlayer
直接输出到 Surface —— 桌面壁纸上播视频是最省电的做法，解码全在系统侧。

API 28 以下没有 ImageDecoder，`decodeMediaDrawable` 会退回 BitmapFactory，
GIF 退化成首帧但不会崩。

### 安装流程：给人看的进度，而不是日志

`install/InstallSession.kt` 把「刷入 / 修补 / 装模块」拆成固定阶段（检查环境 → 解包镜像 →
修补内核 → 回写分区 → 校验），每个阶段有状态、说明、耗时；原始输出收进「查看日志」，
默认折叠。失败时只显示一条人话原因 + 补救入口，并把失败定位到具体阶段。

**音量键提问**：`PromptParser` 从脚本输出里识别 `音量上键：… / Volume UP: …` 这类行，
转成 `InstallPrompt`；`InstallPromptCard` 用大按钮呈现选项，同时保留按键提示
（音量上 = 是 · 音量下 = 否 · 电源键 = 确认）。用户点按钮和执行器读按键走同一个入口
`answerPrompt(index)`，不会出现「弹窗点了没反应」的两套状态。

接真实 ksud 时只需替换 `DemoInstallRunner`：读进程输出 → 每行丢给 `PromptParser` →
有提示就 `raisePrompt`，其余照常推进阶段；UI 一行都不用改。

### 导航栏（Dock）

- **单指示器**：选中位置由一个 Float 驱动，在 `drawBehind` 里按行宽算出圆角矩形，
  不再给 4 个 item 各挂一个 `animateFloatAsState`；切换时只有绘制失效，没有 4 份重组。
- **标签常驻**：去掉了 `weight > 0.35` 的显隐判断，文字不再弹进弹出，只用颜色/图标缩放表达选中。
- **热区 ≥ 48dp**，带 `selectableGroup` / `selected` / `contentDescription` 语义，
  切换时给一次轻触感（`TextHandleMove`）。

### 视频壁纸的省电与内存

- 视频壁纸跟随 `Lifecycle` 暂停/恢复，`ON_PAUSE` 立刻 `pause()`，不会在后台继续解码；
- 视频模式下不再绘制底层的程序化壁纸（反正被完全遮住）；
- 图片/GIF 解码带降采样，最长边压到 1440px，4K 原图不会整张进内存。

### 功能（这轮新增）

- **明暗外观**：设置页一键切换，浅色走 `huiColors(dark = false)` 的整套色板。
- **设置持久化**：强调色/材质/动效/明暗存 `SharedPreferences`（`NovaSettingsStore`），
  重启后保持；壁纸另有 `NovaWallpaperStore`。
- **返回键**：任意子页按返回先回首页，不会直接退出 Nova。
- **真实设备信息**：首页 Hero 与工具页读取 `Build.*`、`os.version`、`ActivityManager`，
  显示型号/厂商/Android 版本/API/内核/KMI/ABI/内存/核心数，不再用演示数据。

### 修掉的一个重组问题

壁纸相位之前是「组合期读 Float」，12fps 的壁纸等于每秒整屏重组 12 次。
现在 `rememberWallpaperPhase` 返回 `State`，真正的读取发生在 `Canvas` 的绘制 lambda 里，
每秒 12 次只是绘制失效，不触发重组。

## 性能与功耗

- **列表懒加载**：四个屏幕全部改成 `LazyColumn`，屏幕外的玻璃卡不组合、不采样背景图层。
- **极光 4Hz**：漂移用低频状态（250ms 一次）驱动 `graphicsLayer` 平移，渐变只在图层里画一次，
  背景重绘从 60fps 降到 4fps。
- **局部光栅化**：每张玻璃卡用 `clipRect` 把背景采样限制在自己的矩形内，减少 fill-rate 浪费。
- **独立进程**：`NovaActivity` 跑在 `:nova` 进程，`KernelSUApplication` 对该进程直接返回，
  跳过 Koin + Root Shell 的 `runBlocking` 初始化——冷启动不再等那段 IO。
- **首帧底色**：窗口背景预设为 `#07070B`，冷启动不会先闪白再切到极光。

- **生命周期闸门**：`HuiAnimationHost` 观察宿主 `Lifecycle`，只有 `RESUMED` 才创建无限动画。
  退到后台或息屏时，极光漂移、液态高光、呼吸/自转/波浪全部停止，而不是继续空转耗电。
- **玻璃档位**：`HuiMotionLevel` 自动推导 `HuiGlassQuality`——克制档把模糊半径砍半并关闭
  AGSL 折射，张扬档才给满。设置页「玻璃档位」一行会实时显示当前生效档位。
- **系统动画缩放**：Compose 动画读取 `MotionDurationScale`，系统设置里关掉动画时会直接
  跳到终态，不需要额外分支。
- **笔刷复用**：高光、描边两套渐变都在 `remember` 里建一次，避免每帧新建 shader 对象。
- **状态写入收敛**：玻璃卡片只在 `positionInRoot` 真正变化时更新偏移，滚动时不会反复失效。
- **切换动画**：页面切换用 `AnimatedContent`（新页淡入 + 0.97→1 缩放，旧页 160ms 快速淡出），
  两层不同时长，减少同时占用 GPU 的时间。

## 现状与边界

**已经是真的**

- 设计系统、三种材质（柔光/液态/纯色）、1024 个动画变体、卡片族、Dock、动态壁纸
- 壁纸：6 个程序化预设 + 本地图片/GIF/MP4 + 内置初音视频；系统级 WallpaperService
- 真实设备信息（Build / os.version / ActivityManager），不再写死演示机型
- 设置持久化：明暗、强调色、材质、动效、壁纸全部落 SharedPreferences
- 安装流程状态机：阶段推进、失败定位、日志环形缓冲、音量键提问转按钮

**还是演示**

- `DemoInstallRunner`：阶段和提问是模拟的，没有真正调用 ksud
- 首页/工具页的业务动作（备份、救砖、模块管理、超级用户）回调仍为空
- 模块列表、超级用户列表是静态数据

**为什么不直接接真流程**

`:nova` 进程在 `KernelSUApplication` 里早退，没有 Koin、没有 Root Shell。
两条路：把安装放主进程、用 Binder/广播把进度与提问回传（推荐）；或在 `:nova` 里
只启动安装所需的那几个 Koin 模块。

**没验证过**

没有 KVM、没有真机：玻璃观感、动画手感、视频壁纸播放、返回手势，目前只有编译和
单元测试背书。


## 素材来源

- 星云/星场图：OpenGameArt [3-layer parallax star and nebula field](https://opengameart.org/content/3-layer-parallax-star-and-nebula-field)，CC0
- 内置动态壁纸：用户提供（`assets/nova/miku_star.mp4`）
- 图标：全部手写 24×24 路径，无第三方图标库
