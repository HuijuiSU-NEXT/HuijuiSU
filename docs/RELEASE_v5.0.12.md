# HuiJuiSU NEXT Ultra v5.0.12

> 修复版：解决「点功能按钮后一直卡在正在安装 / 正在扫描 / 查询中」的问题。

| | |
|---|---|
| 版本号 | `v5.0.12 (35359)` |
| 基线 | v5.0.11 |
| 包名 | `com.huijui.sunext.ultra`（覆盖安装） |
| APK | 5 个架构，38.2 – 50.0 MB |
| 官网 | https://huijuisu-next.github.io/ |

---

## 一、根因：libsu 调用没有超时

之前的实现是：

```kotlin
Shell.cmd(cmd).exec()   // 阻塞，且没有超时
```

一旦 su 授权弹窗没处理、或内核响应慢，这个调用会**永久阻塞**，界面就永远停在：

```
正在安装隐藏模块…
正在扫描…
查询中…
```

更糟的是这些调用是**并发**发起的——多个卡片各自起线程请求 su，KernelSU 会排队甚至互相等待，越点越卡。

模块停在 v1.3 也是同一个原因：安装命令根本没执行完。

---

## 二、修复内容

### 1. 所有 root 调用加超时

```kotlin
val task = pool.submit { Shell.cmd(cmd).exec() }
try { task.get(timeoutSeconds, TimeUnit.SECONDS) }
catch (TimeoutException) { task.cancel(true); return "__TIMEOUT__" }
```

超时返回标记，调用方转成可读提示，不再无限等待。

### 2. root 调用串行化

线程池从 `newCachedThreadPool` 改为 **`newSingleThreadExecutor`**：

| 之前 | 现在 |
|---|---|
| 多个 su 并发提交 | 一次只跑一个 |
| 容易互相阻塞 | 顺序执行，各自超时 |
| 越点越卡 | 行为可预测 |

### 3. UI 层兜底

每张卡片的执行再包一层：

```kotlin
withTimeoutOrNull(150_000L) { ... } ?: false
```

无论底层如何，**最多 150 秒一定回到界面**并给出结果。

### 4. 安装动作单独超时

模块安装 120 秒超时会明确提示：

```
安装超时：root 无响应，请确认管理器已被授予 root
```

---

## 三、发布流程优化

| 项 | 改动 | 效果 |
|---|---|---|
| Telegram Bot API 编译 | 停用（上传 APK 功能早已关闭） | 单次发布省 **20+ 分钟** |
| CodeQL 扫描 | 停用 | 不再每次 push 报红 |
| 发布链路 | Build Manager → Release → notify | 从 40+ 分钟降到约 20 分钟 |

---

## 四、验证方法

装好后按顺序检查：

### 1. Root 状态

工具箱第一张卡片，点一下应显示类似：

```
code=0 APP_UID=10352 /system/bin/su SU_UID=0 /data/adb/ksu/bin/ksud
```

| 输出 | 含义 |
|---|---|
| 有 `SU_UID=0` | root 正常，继续下一步 |
| `__TIMEOUT__` | su 链路不通，检查内核授权 |
| `Permission denied` | 管理器未被内核认可 |

### 2. 一键隐藏

- 正常：几十秒内出结果（已安装 / 需重启）
- 异常：150 秒内必给提示，不会无限转圈

### 3. 模块版本

安装后重启，模块列表里 `hide_bundle` 应从 **v1.3** 变为 **v1.4-super**。

---

## 五、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.7 MB |
| `armeabi-v7a` | 老 32 位设备 | 38.2 MB |
| `x86_64` | 模拟器 / 平板 | 40.2 MB |
| `riscv64` | RISC-V | 38.6 MB |
| `universal` | 不确定选这个 | 50.0 MB |

---

## 六、仍未改变的限制

| 项 | 说明 |
|---|---|
| 热刷入范围 | 仅纯 VFS 文件计划的模块可免重启 |
| 元模块前置 | 需先安装 Hybrid Mount 并重启一次 |
| 核心模块保护 | 内置隐藏模块禁止卸载与停用 |

---

<sub>完整对比：`git log v5.0.11..v5.0.12`</sub>
