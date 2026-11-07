# HuiJuiSU NEXT Ultra v6.1

> **一句话**：管理器整体重写为 Nova 界面，经典界面下线，release 构建链彻底修通，并带来四项同类产品没有的能力。

| | |
|---|---|
| 版本号 | `v6.1 (35374)` |
| 包名 | `com.huijui.sunext` |
| 体积 | 58–70 MB（5 个架构） |
| 最低系统 | Android 8.0（API 26） |
| 许可 | GPL-3.0 |

---

## 目录

- [一、这次到底更新了什么](#一这次到底更新了什么)
- [二、界面：为什么换掉第三方组件库](#二界面为什么换掉第三方组件库)
- [三、独有能力 ①：一键体检](#三独有能力--一键体检)
- [四、独有能力 ②：自愈中心](#四独有能力--自愈中心)
- [五、独有能力 ③：补丁中心](#五独有能力--补丁中心)
- [六、独有能力 ④：SUSFS 面板](#六独有能力--susfs-面板)
- [七、4.x 非 GKI 兼容模式](#七4x-非-gki-兼容模式)
- [八、其他改进](#八其他改进)
- [九、内核支持范围](#九内核支持范围)
- [十、安装与升级](#十安装与升级)
- [十一、已知限制](#十一已知限制)

---

## 一、这次到底更新了什么

| 类别 | 内容 |
|---|---|
| **界面** | 全部重写为 Nova：四标签管理器 + 深色玻璃材质 + 动态壁纸 |
| **构建** | 修复长期存在的 release 构建失败（dex 重复类），现在能正常出包 |
| **依赖** | 移除第三方组件库（Miuix），Nova 只依赖 Compose `foundation` |
| **删减** | 经典界面 131 个文件下线，`MainActivity` 变为 Nova 入口 |
| **改名** | 包名 `org.bakasu.bakasu` → `com.huijui.sunext` |
| **版本** | 版本号改为跟随 git tag，不再写死 |
| **新功能** | 一键体检 · 自愈中心 · 补丁中心 · SUSFS 面板 |
| **生态** | 在线模块仓库（7 条镜像）· 中英切换 · 更新检查 |
| **内核侧** | `ksud` 新增 `profile revoke`（走内核 `app_profile` ioctl） |

---

## 二、界面：为什么换掉第三方组件库

旧的 Nova 界面建立在 Miuix 之上。它的 KMP 产物在 AGP 9 下会被 **dex 两次**：

```
Type top.yukonga.miuix.kmp.utils.PagerDragEvent is defined multiple times
  project_dex_archive/.../PagerDragEvent.dex
  external_libs_dex/.../classes5.dex
```

这导致 release 构建长期失败，只能出 debug 包。

### 这一版的处理

1. Nova 界面改为**自研 NovaUI**，只依赖 Compose `foundation`
2. 经典界面（`ui/ data/ domain/ di/ profile/ magica/`）整体移出源码集
3. `MainActivity` 重写为 15 行 Nova 入口
4. `KernelSUApplication` 重写为最小初始化（去掉 Koin 与 Root Shell 预热，冷启动更快）
5. Miuix 依赖 5 个全部删除
6. `AndroidManifest` 只保留 4 个组件

### 视觉上的变化

| 之前 | 现在 |
|---|---|
| 白色磨砂卡片，深色壁纸下"白雾 + 灰字" | **深色玻璃**：顶部 26% 高光、中部透出壁纸、底边压暗 |
| Dock 用 `shadow`，塑料托盘感 | Dock 同一套玻璃材质，无 `shadow` |
| 时钟 46sp 常规字距 | 52sp + −2sp 字距，接近系统锁屏观感 |
| 卡片、Dock、按钮三种材质各说各话 | 全局统一一套玻璃语言 |

| 首页 | Dock 细节 |
|---|---|
| ![首页](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/home.png) | ![Dock](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/dock.png) |

> 图片走 jsDelivr，国内可访问；加载失败可到仓库 `docs/images/v6.1/` 查看原图。

---

## 三、独有能力 ①：一键体检

同类管理器只给**清单**（装了哪些模块、谁有 root），不给**结论**。

一键体检做五件事：

| 检查项 | 具体做什么 |
|---|---|
| **KernelSU 状态** | 识别 未工作 / LKM / GKI 内置 / 4.x 非 GKI |
| **模块文件冲突** | 遍历每个模块的 `system/` 路径，找出两个模块覆盖**同一个文件**的情况 —— 这是开机卡 logo 最常见的原因 |
| **隐藏检测** | 内联春秋检测，统计系统属性 / 挂载 / maps 的暴露项 |
| **boot 备份** | 是否存在可回滚的备份目录 |
| **模块数量** | ≥15 个时提示启动变慢、冲突概率上升 |

评分：基线 100 分，每个严重项 −25，每个提醒项 −10。

**每一条结论右侧都有直达入口**：看报告 / 去自愈 / 去刷写 / 看模块。

![一键体检](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/checkup.png)

---

## 四、独有能力 ②：自愈中心

体检负责"发现问题"，自愈中心负责"解决问题"。

| 能力 | 实现 | 可逆性 |
|---|---|---|
| **冲突一键修复** | 每个冲突路径保留第一个模块，停用后加载的那些 | ✅ 删掉 `disable` 即恢复 |
| **单模块停用** | `touch /data/adb/modules/<id>/disable` | ✅ |
| **单模块恢复** | `rm -f .../disable` | ✅ |
| **清理缓存** | 清 `/data/local/tmp`、`/data/adb/ksu/log` | ✅ 无副作用 |
| **最近安装模块** | 按目录 mtime 列出 TOP3，开机异常时优先停用 | ✅ |

**实际价值**：刷了模块开不了机，传统做法要进 TWRP 或用另一台设备 adb 删文件。自愈中心可以在"还能开机"的阶段把风险模块停掉，且完全可逆。

---

## 五、独有能力 ③：补丁中心

12 条内置补丁，全部走 `resetprop`，**重启自动恢复**，不写分区。

### 系统属性

| 补丁 | 行为 |
|---|---|
| 隐藏可调试标记 | `ro.debuggable=0`、`ro.secure=1` |
| 还原正式构建标签 | `ro.build.tags=release-keys`（藏 test-keys） |
| 构建类型改为 user | `ro.build.type=user`（藏 userdebug） |
| 归一化构建者信息 | `ro.build.user` / `ro.build.host` 改通用值 |

### 引导状态

| 补丁 | 行为 |
|---|---|
| 伪装已上锁 BL | `ro.boot.verifiedbootstate=green`、`ro.boot.flash.locked=1` |
| 伪装 dm-verity 正常 | `ro.boot.veritymode=enforcing` |
| 伪装 vbmeta 已锁定 | `ro.boot.vbmeta.device_state=locked` |

### 调试与环境

| 补丁 | 行为 |
|---|---|
| 关闭 adb root 标记 | `service.adb.root=0` |
| 抹掉模拟器特征 | `ro.kernel.qemu=""`、`ro.hardware` 归一到 qcom |
| 恢复 ro.secure | `ro.secure=1` |

### 其他

| 补丁 | 行为 |
|---|---|
| 重启使补丁失效 | 直接重启（临时属性全部恢复） |
| 查看已修改属性 | 一次性列出 8 个关键属性当前值 |

风险分级：安全 / 谨慎 / 危险。「谨慎」会弹确认框并列出将修改的属性。

![补丁中心](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/patches.png)

---

## 六、独有能力 ④：SUSFS 面板

把 `ksud susfs config` 的能力全部放进界面：

| 能力 | 对应命令 |
|---|---|
| 总开关 | `susfs config enable` / `disable` |
| 日志 | `susfs config logging add` / `remove` |
| AVC 日志伪造 | `susfs config avc_log_spoofing add` / `remove` |
| 隐藏 sus 挂载 | `susfs config hide_sus_mnts_for_non_su_procs add` / `remove` |
| 隐藏路径增删 | `susfs config sus_path add <path> --loop` / `remove <path>` |
| 导出配置 | `susfs config backup` |
| 恢复默认 | `susfs config restore` |

内核未编译 susfs 时，页面明确提示「只能看不能改」，不给点了没反应的假开关。

![SUSFS](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/susfs.png)

---

## 七、4.x 非 GKI 兼容模式

4.x 内核没有 GKI、没有模块化构建，**LKM 方案加载不了**。这一版做了专门页面：

- 解释为什么 4.x 用不了 LKM
- 给出两条可行路线：
  1. 修补现成 `boot.img`（有官方镜像时最快）
  2. 编内核补丁（有内核源码时最彻底）
- 内置可复制的编译命令（拉源码 → 打 KernelSU 补丁 → 编译 → fastboot 刷入）

检测到 4.x 内核时，首页「工作模式」会自动变为可点击，提示进入该模式。

![4.x 模式](https://cdn.jsdelivr.net/gh/HuijuiSU-NEXT/HuijuiSU@main/docs/images/v6.1/legacy4x.png)

---

## 八、其他改进

### 在线模块仓库

接入 [`HuijuiSU-NEXT/Huijui_Module_Repo`](https://github.com/HuijuiSU-NEXT/Huijui_Module_Repo)：

- **7 条镜像自动回退**：GitHub → GitHub API → jsDelivr → ghproxy → gh-proxy → gitmirror → ghproxy.cn
- 单条连接 8 秒 / 读取 20 秒超时，失败立即换下一条
- 内容必须以 `[` 开头才认（防止镜像返回 HTML 错误页）
- 界面显示命中的镜像名
- 25 秒看门狗兜底，不会一直转圈

### 中英切换

- 196 条词典，中文为 key，英文查表
- 查不到自动回退中文，**不会出现空白或乱码**
- SharedPreferences 持久化，重启保持
- 入口：设置 → 语言 → 界面语言

### 更新检查

- 3 条镜像回退
- 结果缓存 6 小时
- 版本号按段比较（`v6.0` < `v6.1` 正确判断）
- 离线时用过期缓存兜底并标注

### 包名与版本

| 项 | 变化 |
|---|---|
| 包名 | `org.bakasu.bakasu` → `com.huijui.sunext`（含 aidl / baselineprofile / lint-rules / namespace） |
| applicationId | **不变**，覆盖安装不丢数据 |
| 版本号 | 跟随 git tag 自动生成（`GITHUB_REF_NAME`），不再写死 |

### ksud

新增 `ksud profile revoke <package> <uid>`，走内核 `app_profile` ioctl 撤销授权；管理器中的撤销操作优先走它，失败自动回退到改授权表。

---

## 九、内核支持范围

| 内核系列 | 支持方式 | CI 产物 |
|---|---|---|
| 6.x（android15-6.6 / 16-6.12 / 17-6.18） | LKM | ✅ kernelsu.ko |
| 5.10 / 5.15（android12 / 13 / 14） | LKM | ✅ kernelsu.ko |
| 4.x（4.9 / 4.14 / 4.19） | 内核补丁 | ❌ 无 .ko（内核不支持模块化） |
| 非 GKI 5.x | 内核补丁 | ❌ 同上 |

本次 CI 构建 **16 个 LKM 目标** + **4 个架构的 ksud**，全部成功。

---

## 十、安装与升级

### 升级（已有旧版）

直接覆盖安装即可，`applicationId` 未变，数据保留；桌面只有一个图标。

### 全新安装

1. 下载对应架构 APK（不确定就用 `universal`）
2. 安装后打开，直接进入 Nova 界面
3. 首次进入「模块」页需要授予 root

### 架构对应

| 架构 | 适用设备 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数现代手机 | 61.0 MB |
| `armeabi-v7a` | 老 32 位设备 | 58.5 MB |
| `x86_64` | 模拟器 / 部分平板 | 60.5 MB |
| `riscv64` | RISC-V 设备 | 58.9 MB |
| `universal` | 不确定时用这个 | 70.3 MB |

sha256 见下方 Assets 列表。

---

## 十一、已知限制

| 项 | 说明 |
|---|---|
| **安全模式** | 由开机时音量键触发，属于内核层，管理器没有写入接口，因此不提供假开关 |
| **KPM** | 本 fork 内核侧未实现 KPM 加载器，userspace 无法单独支持 |
| **多语言** | 覆盖框架与常用项（196 条），长描述句仍为中文 |
| **4.x 支持** | 提供引导与 boot 修补工具链；真正编译内核仍需设备内核源码 |
| **真机待验证** | 在线仓库的网络连通性、`ksud profile revoke` 的实际写入，需在真机确认 |

---

## 反馈

- Telegram 群：[t.me/Huijui123](https://t.me/Huijui123)
- GitHub Issues：[提交问题](https://github.com/HuijuiSU-NEXT/HuijuiSU/issues)
- 完整许可清单：[docs/THIRD_PARTY_NOTICES.md](https://github.com/HuijuiSU-NEXT/HuijuiSU/blob/main/docs/THIRD_PARTY_NOTICES.md)

---

<sub>完整变更：`git log v6.0..v6.1` · 构建日志：GitHub Actions run 42359703852</sub>
