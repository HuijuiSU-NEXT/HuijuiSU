# HuiJuiSU NEXT Ultra v5.0.3

> 修复版：解决「版本号永远不变」与「装了元模块却显示未检测到」两个长期问题。

| | |
|---|---|
| 版本号 | `v5.0.3 (35339)` |
| 基线 | v5.0.2 |
| 包名 | `com.huijui.sunext.ultra`（覆盖安装） |
| APK | 5 个架构，37.6 – 49.4 MB |

---

## 一、版本号一直显示 v4.5.1-rc1

### 现象

不管发布多少版，关于页都显示：

```
当前版本：v4.5.1-rc1 (35322)
升级版本：v4.5.1-rc1 (35337)
```

只有括号里的 versionCode 在变。

### 根因

`app/build.gradle.kts` 里把版本名写死了：

```kotlin
versionName = "v4.5.1-rc1"     // ← 硬编码
```

而根目录 `build.gradle.kts` 里明明维护着：

```kotlin
extra["managerVersionName"] = "v5.0.2"
```

上游从来没读这个变量。

### 修复

```kotlin
versionName = managerVersionName
```

本版起，关于页会正确显示 `v5.0.3`，更新弹窗也能正确比较版本。

---

## 二、装了元模块却提示未检测到

### 现象

Hybrid Mount 已安装并生效，工具箱里「元模块状态」仍提示：

```
未检测到元模块 Hybrid Mount
```

### 根因

`HotModule` 判断文件是否存在时用的是应用进程：

```kotlin
File("/data/adb/modules/hybrid_mount/module.prop").isFile   // 永远 false
```

应用进程对 `/data/adb` 没有读权限，`isFile` 必然返回 false。

v5.0.2 修过 `HuijuiBuiltinBundle` 的同类问题，但 `HotModule` 漏了。

### 修复

改用 root 探测：

```bash
test -f modules/hybrid_mount/module.prop && echo __ACTIVE__
test -x modules/hybrid_mount/hybrid-mount && echo __BIN__
test -f modules_update/hybrid_mount/module.prop && echo __UPDATED__
```

二进制查找也改用 root 的 `test -x` 逐个尝试。

---

## 三、Evil Service 自查同样误判

`runEvilCheck` 里也是：

```kotlin
if (!path.isFile) return "内置脚本不可用"
```

改成 root 直接执行脚本，失败时带回真实原因（例如部署阶段的 `cp` 报错）。

同时删除了两个永远返回 null 的死函数（`deviceHideZip` / `deviceEvilScript`）。

---

## 四、排查过的其他位置

| 位置 | 结论 |
|---|---|
| `SuperUserRepository` 读 allowlist | ✅ 用的是 `SuFile`，走 root |
| `HuijuiModuleSnapshot` | ✅ 读写应用自己目录 |
| `ModuleBackupManager` | ✅ 用 `SuFile` |
| 工具箱前 5 张卡 | ✅ 用 libsu |
| 内置资源部署与检查 | ✅ v5.0.2 已改 root 侧 |

---

## 五、升级建议

| 情况 | 建议 |
|---|---|
| 从 v5.0.2 升级 | 直接覆盖安装 |
| 之前一直显示 v4.5.1-rc1 | 本版起版本号正常，无需额外操作 |
| 元模块检测仍异常 | 先点「Root 状态」确认 `SU_UID=0` |

---

## 六、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.2 MB |
| `armeabi-v7a` | 老 32 位设备 | 37.6 MB |
| `x86_64` | 模拟器 / 平板 | 39.6 MB |
| `riscv64` | RISC-V | 38.0 MB |
| `universal` | 不确定选这个 | 49.4 MB |

---

## 七、限制（未变）

| 项 | 说明 |
|---|---|
| **热刷入范围** | 仅纯 VFS 文件计划的模块可免重启 |
| **元模块前置** | 需先安装 Hybrid Mount 并重启一次 |
| **开机激活** | 需要先打开一次管理器完成资源落盘 |

---

<sub>完整对比：`git log v5.0.2..v5.0.3`</sub>
