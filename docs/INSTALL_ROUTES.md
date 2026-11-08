# 安装路线：LKM 路线 / 魔改内核路线

> 先看你的内核属于哪一类，再选路线。**选错路线是绝大多数“管理器装了但没 root”的原因。**

---

## 30 秒判断自己属于哪条路线

在手机终端或 adb 里执行：

```bash
uname -r
```

| 内核版本 | 路线 |
|---|---|
| **5.10 / 5.15 / 6.1 / 6.6 / 6.12 / 6.18**（GKI 2.0） | 👉 **路线 A：LKM** |
| **4.4 / 4.9 / 4.14 / 4.19**（非 GKI） | 👉 **路线 B：魔改内核** |
| 非 GKI 的 5.x（厂商自编译） | 👉 **路线 B：魔改内核** |

再确认一次内核有没有“已经内置” KSU：

```bash
su -c "lsmod | grep -i ksu; ls /sys/module | grep -i ksu"
```

- **有输出** → 内核已经带 KSU，直接用路线 A 的「第 4 步：装管理器」
- **无输出** → 按对应路线完整走

---

# 路线 A：GKI 2.0 + LKM（推荐，绝大多数设备）

**适合**：官方 GKI 内核、能刷 boot 或 init_boot 的设备、Android 12～17。

## 原理

LKM（Loadable Kernel Module）= 把 KernelSU 做成一个可加载模块 `kernelsu.ko`，
不需要重编内核，只要按 KMI 匹配加载即可。

## 步骤

### 1. 确认 KMI

| 内核版本 | KMI（下载对应 ko） |
|---|---|
| 5.10（Android 12） | `android12-5.10_kernelsu.ko` |
| 5.10（Android 13） | `android13-5.10_kernelsu.ko` |
| 5.15（Android 13） | `android13-5.15_kernelsu.ko` |
| 5.15（Android 14） | `android14-5.15_kernelsu.ko` |
| 6.1（Android 14） | `android14-6.1_kernelsu.ko` |
| 6.6（Android 15） | `android15-6.6_kernelsu.ko` |
| 6.12（Android 16） | `android16-6.12_kernelsu.ko` |
| 6.18（Android 17） | `android17-6.18_kernelsu.ko` |

> 文件名带架构前缀：`aarch64-…`（绝大多数手机）或 `x86_64-…`（模拟器 / 部分平板）。

### 2. 下载对应 ko

在 [Releases](https://github.com/HuijuiSU-NEXT/HuijuiSU/releases) 里找同名资产，
例如 6.12 的 arm64 设备：

```
aarch64-android16-6.12_kernelsu.ko
```

### 3. 加载模块

```bash
adb push aarch64-android16-6.12_kernelsu.ko /data/local/tmp/
adb shell su -c "ksud insmod /data/local/tmp/aarch64-android16-6.12_kernelsu.ko"
adb shell su -c "lsmod | grep -i ksu"     # 有 kernelsu 就成功了
```

也可以把 ko 交给 KernelSU 管理器刷入（管理器里「安装」→ 选 ko）。

### 4. 安装管理器

装 `HuiJuiSU_NEXT_Ultra_*.apk`（arm64 设备选 `arm64-v8a`，不确定选 `universal`）。

打开后应该看到：

- 首页「工作模式 · LKM」
- 模块页能列出已装模块
- 超级用户页能读到授权表

### 5. 验证

```bash
adb shell su -c id
```

返回 `uid=0` 说明链路通了；此时管理器里所有 root 功能可用。

---

# 路线 B：非 GKI / 魔改内核（内核补丁）

**适合**：4.x 内核、厂商魔改内核、非 GKI 5.x、以及任何 LKM 加载不了的设备。

## 原理

KernelSU 直接编进内核镜像，不依赖模块加载。代价是：**每次换内核都要重新打补丁编译**。

## 步骤

### 1. 准备

- 设备对应的**内核源码**（官方或厂商开源仓库）
- 编译环境（AOSP 工具链 / 厂商 build.sh）
- 原始 `boot.img`（刷坏了能回滚）

### 2. 应用 KernelSU 补丁

```bash
git clone --depth=1 <你的内核仓库> -b <分支> kernel && cd kernel
curl -LSs "https://raw.githubusercontent.com/HuijuiSU-NEXT/HuijuiSU/main/kernel/setup.sh" | bash -s main
```

### 3. 编译

用设备原本的编译脚本（`build.sh` / `make`），产出新的 `boot.img` 或 `Image.gz-dtb`。

### 4. 刷入

```bash
fastboot flash boot out/arch/arm64/boot/Image.gz-dtb
# 或
fastboot flash boot new-boot.img
```

### 5. 安装管理器

同上，装 `HuiJuiSU_NEXT_Ultra_*.apk`。

打开后首页应显示：

- 「工作模式 · 4.x 内核」或「GKI 内置」
- 若是 4.x，管理器会自动引导到「4.x 兼容模式」页

---

## 两条路线对比

| | 路线 A（LKM） | 路线 B（魔改内核） |
|---|---|---|
| 支持内核 | GKI 2.0（5.10+） | 任意（含 4.x） |
| 需要重编内核 | ❌ 不需要 | ✅ 需要 |
| 换内核后 | 重新加载对应 ko | 重新打补丁编译 |
| 难度 | 低 | 高 |
| 风险 | 低（可 rmmod 卸载） | 中（刷 boot，需备份） |
| 管理器识别方式 | ko 里带签名哈希 | 内核里带签名哈希 |

---

## 常见问题

### 装了管理器但没有 root / 模块列表为空

1. 先看首页顶部是否出现红色提示 —— 现在会直接说明原因
2. 点进「诊断」页，复制信息
3. 常见原因：
   - **内核没内置 KSU，也没加载 ko** → 走路线 A 第 3 步
   - **ko 与 KMI 不匹配** → 换对应版本的 ko
   - **管理器签名不被内核认可** → 说明内核是别处构建的，需要用它认识的管理器授权一次，或刷本项目内核

### `insmod` 报 `File exists` / 符号冲突

内核已经内置 KSU，不需要再加载 ko。直接装管理器即可。

### 4.x 设备能不能用 LKM？

**不能。** 4.x 没有 GKI、没有模块化构建，`kernelsu.ko` 加载不了。只能走路线 B。

### 刷了路线 B 的内核后还能回退吗？

可以。保留原始 `boot.img`，`fastboot flash boot 原boot.img` 即可。

---

## 相关文档

- [内核支持范围](https://github.com/HuijuiSU-NEXT/HuijuiSU#内核支持)
- [真机验证清单](VERIFY.md)
- [第三方许可](THIRD_PARTY_NOTICES.md)
