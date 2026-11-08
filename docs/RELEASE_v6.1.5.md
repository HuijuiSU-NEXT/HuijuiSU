# HuiJuiSU NEXT Ultra v6.1.5

> **这一版解决的是“装了但用不起来”**：管理器与内核的对接方式、授权表读取、模块列表解析全部按内核真实格式重写；同时把 `kernelsu.ko` 一起发出来，GKI 设备可以直接用 LKM。

| | |
|---|---|
| 版本号 | `v6.1.5 (35397)` |
| 包名 | `com.huijui.sunext`（未变，可覆盖安装） |
| APK | 5 个架构，58.5–70.3 MB |
| 内核模块 | **16 个** `kernelsu.ko`（8 个 KMI × 2 架构） |
| 最低系统 | Android 8.0（API 26） |

---

## 一、先看这个：两条安装路线

选错路线是“管理器装了却没有 root”的最常见原因。

| 你的内核 | 路线 |
|---|---|
| **GKI 2.0**（5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18） | **路线 A：LKM**，加载对应 `kernelsu.ko`，不用重编内核 |
| **4.x / 非 GKI 魔改内核** | **路线 B：内核补丁**，需要内核源码重编 boot |

完整命令与排错 👉 [docs/INSTALL_ROUTES.md](https://github.com/HuijuiSU-NEXT/HuijuiSU/blob/main/docs/INSTALL_ROUTES.md)

---

## 二、内核模块（本版新增随包发布）

`kernelsu.ko` 按 KMI 与架构命名，直接选对应文件：

| KMI | Android | aarch64 | x86_64 |
|---|---|---|---|
| `android12-5.10` | 12 | ✅ | ✅ |
| `android13-5.10` | 13 | ✅ | ✅ |
| `android13-5.15` | 13 | ✅ | ✅ |
| `android14-5.15` | 14 | ✅ | ✅ |
| `android14-6.1` | 14 | ✅ | ✅ |
| `android15-6.6` | 15 | ✅ | ✅ |
| `android16-6.12` | 16 | ✅ | ✅ |
| `android17-6.18` | 17 | ✅ | ✅ |

加载方式：

```bash
adb push aarch64-android16-6.12_kernelsu.ko /data/local/tmp/
adb shell su -c "ksud insmod /data/local/tmp/aarch64-android16-6.12_kernelsu.ko"
adb shell su -c "lsmod | grep -i ksu"
```

> 内核若已内置 KernelSU（`lsmod` 已有 ksu），**不要**再 insmod，直接用管理器授权即可。

---

## 三、稳定性修复（本版重点）

### 1. 超级用户：按内核真实格式读授权表

之前把 `/data/adb/ksu/.allowlist` 当文本解析，实际它是二进制：

```
u32 magic("USK\x7f") + u32 version + N × 784 字节 struct app_profile
```

现在按结构体偏移解析（`key@4`、`curr_uid@260`、`allow_su@264`），
授权列表**自动全部显示并置顶**，不再需要手动添加。

### 2. 模块：解析不再错位

`module.prop` 的 `description` 里经常有 `|`，旧代码用 `|` 分隔会把字段挤乱，
导致模块名空白、状态错乱。现在改用 `##<id>` 分段解析，并顺带识别
`webroot/`（WebUI 模块）。

### 3. root 调用：串行化 + 脚本化

- 所有 `su` 调用加全局互斥，避免并发被内核拒绝
- 多行脚本改为 base64 单行写盘执行（`su -c "<多行>"` 在部分设备上会失败）
- 失败自动重试一次

### 4. 拿不到 root 时，界面直说原因

新增 `RootCheck`，首页顶部横幅会显示：

- `root 被拒绝：请在 KernelSU 管理器里给本应用授权`
- `无法调用 su：本应用未被内核识别为管理器，或设备没有 KernelSU`

点横幅直接跳「诊断」页。

### 5. 诊断页（新）

一次采集：包名 / 版本 / root 状态 / 内核 / 工作模式 / ksud 路径与版本 /
ksu 目录 / 授权表大小 / 模块数量，**一键复制**发给开发者即可定位。

### 6. 模块 WebUI（新）

模块展开后若有 `webroot/index.html`，会出现「打开 WebUI」：
把 webroot 复制到应用缓存后用 WebView 加载（JS / DOM Storage / 本地文件访问全开）。

---

## 四、界面

- 深色玻璃卡片（顶部高光 + 中部透壁纸 + 底边压暗），Dock 同一材质
- 壁纸等比 cover，不拉伸；视频用高清源
- Dock 支持左右拖动，指示器液态玻璃
- 深浅色在壁纸模式下也有明显差异（黑浓度 30% / 18%）
- 中 / 英切换（196 条词典，持久化）

![首页](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1.2/home.png)

---

## 五、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 61.1 MB |
| `armeabi-v7a` | 老 32 位设备 | 58.5 MB |
| `x86_64` | 模拟器 / 平板 | 60.6 MB |
| `riscv64` | RISC-V | 58.9 MB |
| `universal` | 不确定选这个 | 70.3 MB |

覆盖安装即可，`applicationId` 未变，数据保留。

---

## 六、仍未解决

| 项 | 说明 |
|---|---|
| **安全模式** | 内核层行为（开机音量键），管理器无写入接口 |
| **KPM** | 本 fork 内核未实现加载器 |
| **App 内多语言** | 界面仅中 / 英（README 已 6 语言） |
| **4.x 支持** | 提供引导与工具链，编译内核仍需设备内核源码 |

---

<sub>完整对比：`git log v6.1.4..v6.1.5`</sub>
