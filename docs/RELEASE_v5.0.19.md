# HuiJuiSU NEXT Ultra v5.0.19

> 解决「一键隐藏装完还是 v1.3」：ksud 安装不生效时，直接替换模块目录。

| | |
|---|---|
| 版本号 | `v5.0.19 (35372)` |
| 基线 | v5.0.18 |
| 包名 | `com.huijui.sunext.ultra`（覆盖安装） |
| APK | 5 个架构，38.2 – 50.0 MB |
| 官网 | https://huijuisu-next.github.io/ |

---

## 一、为什么一直是 v1.3

KernelSU 装模块分两步：

```
ksud module install   → 写入 /data/adb/modules_update/hide_bundle
重启                  → 用 modules_update 覆盖 modules/hide_bundle
```

第二步在你这个内核版本上**没有发生**（旧模块 ID 一直被占用），所以：

- 安装返回「成功」
- 重启后模块列表还是 v1.3
- `modules_update` 里其实躺着 v1.4-super

---

## 二、这版的修法

### 1. 强制卸载旧版再装

```bash
rm -rf /data/adb/modules/hide_bundle \
       /data/adb/modules_update/hide_bundle
```

连运行中的目录一起删，避免旧模块继续占位。

### 2. 安装结果以退出码为准

```bash
$ksud module install '<zip>' 2>&1; echo __EXIT__$?
```

```kotlin
val ok = out.contains("__EXIT__0")
```

不再用「输出里没有 error 就算成功」这种会误判的写法。

### 3. 安装后校验实际版本

```bash
grep '^version=' /data/adb/modules_update/hide_bundle/module.prop
```

读不到就直接报错，不会假报成功。

### 4. 新增兜底：直接解压到模块目录

ksud 不生效时，改用 App 侧逐文件写入：

```kotlin
ZipInputStream(zip).use { zin ->
    while (entry != null) {
        if (!entry.isDirectory && !name.startsWith("META-INF/")) {
            SuFileOutputStream.open("/data/adb/modules/hide_bundle/$name").use { out ->
                zin.copyTo(out)
            }
        }
    }
}
```

不用设备上的 `unzip`、不依赖 ksud 的更新机制，写完直接 `chmod -R 755` 并校验 `module.prop`。

结果提示会明确区分：

| 提示 | 含义 |
|---|---|
| `已强制替换为 v1.4-super，重启后生效` | ksud 安装成功 |
| `ksud 未生效，已直接替换模块目录（v1.4-super）` | 走兜底，同样有效 |

---

## 三、一并修复的工具箱问题

| 问题 | 修法 |
|---|---|
| 春秋词条卡在「正在执行」 | 脚本是交互式的，改为**只复制到 Download**，由用户用 MT 以 root 运行 |
| 点完春秋后其它卡片一起卡住 | 子进程 stdin 关闭 + 200 秒看门狗强制释放 |
| 一键隐藏提示「未就位」但文件明明在 | 部署改为四个 `test -s` 显式校验，返回 `__DEPLOY_OK__` / `__DEPLOY_FAIL__` |
| root 调用永久阻塞 | 多路径 `su -c` 优先，其次 `ksud debug su`，最后才用 libsu |

---

## 四、验证

装好后：

1. 工具箱 → **Root 状态** → 应显示 `granted : true`，`su` 行 OK
2. **一键隐藏** → 出现上面两种提示之一即成功
3. **重启手机**
4. 模块列表应显示：

```
一键隐藏（78 整合 · 恋雪）超级定制版
版本：v1.4-super
```

---

## 五、手动验证命令

如果 App 提示失败，可以直接在 MT 终端跑：

```bash
su -c 'rm -rf /data/adb/modules/hide_bundle /data/adb/modules_update/hide_bundle'
su -c '/data/adb/ksu/bin/ksud module install /data/adb/huijui/hide_bundle_v1.4_super.zip'
su -c 'ls -l /data/adb/modules_update/hide_bundle'
```

- 第三条能列出内容 → 重启即可
- 第三条报错 → 是 ksud 侧问题，把报错发到 Issue

---

## 六、下载

| 架构 | 适用 | 大小 |
|---|---|---|
| `arm64-v8a` | 绝大多数手机 | 40.7 MB |
| `armeabi-v7a` | 老 32 位设备 | 38.2 MB |
| `x86_64` | 模拟器 / 平板 | 40.2 MB |
| `riscv64` | RISC-V | 38.6 MB |
| `universal` | 不确定选这个 | 50.0 MB |

---

<sub>完整对比：`git log v5.0.18..v5.0.19`</sub>
