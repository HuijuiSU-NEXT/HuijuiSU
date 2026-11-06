# hide_bundle 接入约定（v1）

给模块作者看的：下面是 HuiJuiSU / Nova 管理器当前**实际读取和调用**的东西。
只要这些保持稳定，你更新脚本不需要动管理器；反过来改路径或参数名，管理器这边就需要跟着改。

## 1. 管理器读什么

| 内容 | 路径 | 用途 |
|---|---|---|
| 模块标识 | `/data/adb/modules/hide_bundle/module.prop` | 读 `id` `name` `version` `versionCode` |
| 启用状态 | `.../disable` 文件是否存在 | 存在 = 已被管理器禁用 |
| 子模块 | `/data/adb/modules/{zygisksu,zygisk_lsposed,hma_oss_zygisk,oh_my_keymint}` | 逐个判断目录是否存在，展示「已装 / 缺失」 |
| 日志 | `/data/adb/hide_bundle.log` | 「查看日志」按钮 `tail -n 120` |
| 名单 | `.../lists/generated_blacklist.txt`、`generated_whitelist.txt` | 后续版本会直接展示 |

## 2. 管理器调什么

每次操作都是一条 `su -c "<命令>"`，读取 stdout/stderr 逐行回显，**以退出码判断成败**。

| 按钮 | 实际执行 |
|---|---|
| 一键补装 | `sh /data/adb/modules/hide_bundle/oneclick.sh` |
| 强制重装 | `... oneclick.sh --force` |
| 重导配置 | `... oneclick.sh --reimport` |
| 扫描名单 | `... applist.sh --apply` |
| 查看日志 | `tail -n 120 /data/adb/hide_bundle.log` |
| 刷新状态 | 重新读上面的状态字段 |

## 3. 对脚本的要求

1. **退出码要准**：`0` 表示这一步成功，非 0 我会在界面上标红。
2. **不要用 `read` 等待输入**：管理器是非交互执行的，等输入会挂住。
   需要用户选择时，请把提示打到 stdout，格式照你现在的习惯写就行，例如：
   ```sh
   echo "音量上键：写入 init_boot"
   echo "音量下键：只生成修补包"
   ```
   管理器的 `PromptParser` 会识别这类行并弹按钮（点按钮等价于按音量键）。
3. **幂等**：`--force` / `--reimport` 之外，重复执行同一命令不应破坏已导入的配置。
4. **长任务分段输出**：每完成一步打一行 `[ok] xxx` / `[!!] xxx`，界面按行流式显示，用户能看到进度。
5. **不要 reboot**：需要重启生效时，输出一行 `[reboot-required]` 让用户自己决定。

## 4. 管理器这边已经实现

- 状态检测（安装 / 禁用 / 版本 / 四个子模块）
- 六个操作入口 + 输出流
- 无 Root 时按钮仍可点，但执行结果会如实显示失败原因
- 后续可以接：把 `lists/*.txt` 渲染成可编辑名单、把 `[ok]` 行映射成进度条

## 5. 已知限制

- 管理器**不会**校验 `payload/*.zip` 的签名或哈希，安装来源安全性由用户自己负责
- 非 root 环境下所有读取都会失败，界面显示「缺失」而不是报错
- 目前只做「调用」，不复制你的任何脚本逻辑
