# HuiJuiSU NEXT Ultra v5.0.0

> 这是一次**大版本**：把隐藏能力、元模块、热刷入、开机自启全部收进管理器，同时做了功耗、启动、稳定性的一轮系统性优化。

| | |
|---|---|
| 版本号 | `v5.0.0 (35332)` |
| 基线 | KernelSU 4.5.1-rc1 下游分支 |
| 包名 | `com.huijui.sunext`（覆盖安装不丢数据） |
| APK | 5 个架构，37.6 – 49.4 MB |
| 最低系统 | Android 8.0（API 26） |

---

## 一、内置资源（随 APK 分发，不需要手动导入）

首次启动自动部署到 `/data/adb/huijui/`，版本号变化时自动覆盖更新：

| 资源 | 大小 | 用途 |
|---|---|---|
| `hide_bundle_v1.3.zip` | 27 MB | 一键隐藏模块（恋雪 v1.3，整合 Zygisk Next / LSPosed / HMA / PIF） |
| `evil_service_check.sh` | 6.4 KB | Evil Service 自查 |
| `chunqiu_fix_v1.zip` | 63 KB | 常见春秋词条修复脚本 |
| `hybrid_mount_6.2.5.zip` | 4.8 MB | Hybrid Mount 元模块 |

部署逻辑是幂等的：文件齐全且版本一致时直接跳过，不重复写盘；没有 root 时静默跳过，不打扰用户。

---

## 二、开机秒激活

新增内置模块 `huijui_core`，写入 `/data/adb/modules/huijui_core/`：

| 脚本 | 时机 | 做什么 |
|---|---|---|
| `post-fs-data.sh` | 开机早期 | 首次应用隐藏默认值（`sus_path` / `sus_kstat` / 挂载隐藏 / AVC 伪装），用标记文件保证只跑一次 |
| `boot-completed.sh` | 开机完成 | 元模块缺失则自动安装，再安装隐藏模块；带 `.tried` 标记防重复 |

所以流程变成：

```
刷镜像 / 装 APK → 打开一次（资源落盘 + 写入 huijui_core）→ 重启
→ 开机过程中自动完成隐藏配置与模块安装
→ 不需要再打开管理器
```

安装动作放在 `boot-completed` 而不是 `post-fs-data`，避免拖慢开机。

---

## 三、真·热刷入

接入 Hybrid Mount 的运行时接口：

```
hybrid-mount runtime status|prepare-reboot|load ID|unload ID|reload ID
```

安装模块后不再只是“提示重启”，而是：

1. 从 zip 里读 `module.prop` 拿到模块 id
2. `ksud module install`
3. `hybrid-mount runtime load <id>`
4. 解析返回，判断是否真的生效

| 返回 | 提示 |
|---|---|
| 成功 | 已热激活（无需重启） |
| 混合后端 | 需重启：该模块不是纯 VFS 计划 |
| 找不到 | 热激活失败：模块未安装到位 |
| 元模块缺失 | 装好 Hybrid Mount 后即可热激活 |

> 纯 VFS 文件计划的模块可以免重启挂载；Magic / Overlay 后端仍需重启，这是 Hybrid Mount 自身的设计约束，不是管理器的限制。

---

## 四、工具箱

| 卡片 | 功能 |
|---|---|
| 备份 boot 分区 | `dd` 到 `Download/boot_backup_<时间>.img`，失败会说明原因 |
| 重启 SystemUI | 状态栏卡死时自救 |
| 清理内存缓存 | `drop_caches` |
| 清理临时文件 | `/data/local/tmp` + `modules_update` 残留 |
| 模块备份与还原 | 打包 / 恢复已装模块 |
| 一键隐藏 | 安装内置隐藏模块并尝试热激活 |
| Evil Service 自查 | 扫描后给出「发现 N 处 / 未发现异常」 |
| 元模块状态 | 查询 Hybrid Mount 运行时状态 |
| 安装元模块 | 缺失时一键安装 |
| 春秋词条修复 | 二次确认后执行内置脚本 |
| 重启设备 | 二次确认 |

每张卡片都有**可见执行状态**（执行中 → 结果），并做全局并发保护：同一时间只允许一个任务运行。

---

## 五、性能与功耗

| 项 | 改动 | 效果 |
|---|---|---|
| 背景模糊刷新 | 从“每一帧”改为 **30fps 节流**，并在应用退到后台时停止 | 静止界面 GPU 负载减半，后台归零 |
| 启动隐藏规则 | 十几条规则合并成**一次 su 调用** | 启动进程数 O(n) → O(1) |
| Koin 日志 | release 关闭逐条日志 | 冷启动省几十毫秒 |
| 热启动 | `MainActivity` 改 `singleTask` | 从桌面/最近任务返回不再重建 |
| 壁纸解码 | 按屏幕尺寸封顶（≤2160） | 4K 壁纸内存 48MB → 8MB |
| ksud 调用 | 全部加超时（20s / 180s） | 内核无响应时不再无限转圈 |

---

## 六、稳定性与兼容

| 项 | 说明 |
|---|---|
| 崩溃日志 | 全局 handler 写 `cache/crash/`，保留最近 10 份，含机型/系统/版本/堆栈 |
| 大文件读取 | SUSFS 预览限制 256 KB，避免异常大文件 OOM |
| 死代码清理 | 删除 7 个不可达文件（约 1900 行） |
| 重复实现合并 | 三处 `su` 执行统一到 `HuijuiShellUtils` |
| 折叠屏 / 平板 | `resizeableActivity="true"` |
| 旋转 / 分屏 / 深浅色 | `configChanges` 不再重建 Activity |
| 16KB 页大小 | 已验证 `libkernelsu.so` 的 `p_align = 0x4000` |
| 覆盖范围 | minSdk 26 → targetSdk 37 |

---

## 七、界面

- 新应用图标：透明底、主体居中缩放到 76%，附带圆形版本
- 关于页新增：
  - **加入 QQ 群**：优先用 `mqqapi` 拉起 QQ，失败走官方邀请链接 `qm.qq.com/q/5pS7RgzhJe`
  - **加入 TG 群 / 频道**：弹窗二选一，群 `t.me/Huijui123`，频道 `t.me/HuijuiSU`

---

## 八、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.1 MB |
| `armeabi-v7a` | 老 32 位设备 | 37.6 MB |
| `x86_64` | 模拟器 / 平板 | 39.6 MB |
| `riscv64` | RISC-V | 38.0 MB |
| `universal` | 不确定选这个 | 49.4 MB |

覆盖安装即可，`applicationId` 未变，数据保留。

---

## 九、已知限制

| 项 | 说明 |
|---|---|
| **热刷入** | 仅纯 VFS 计划的模块可免重启，Magic / Overlay 后端仍需重启 |
| **开机激活** | 需要先打开一次管理器完成资源落盘与 `huijui_core` 写入 |
| **keybox / payload** | 体积大且属敏感内容，不随仓库分发 |
| **安全模式** | 内核层行为，管理器无写入接口 |
| **KPM** | 本 fork 内核未实现加载器 |

---

<sub>完整对比：`git log v4.5.1..v5.0.0`</sub>
