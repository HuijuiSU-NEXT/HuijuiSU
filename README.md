# HuiJuiSU

> 基于 KernelSU 的 Root 管理器，面向隐藏与日常可用性做了大量定制。

## 这是什么

HuiJuiSU 是 KernelSU 的下游分支，保留上游的管理器与内核能力，
额外内置了隐藏模块、检测对抗与一批开箱即用的工具。

## 主要能力

| 能力 | 说明 |
|---|---|
| 模块管理 | 安装 / 启停 / 卸载 / 备份，支持真热刷入 |
| 超级用户 | 自动列出全部应用，一键授权 / 撤销 |
| 隐藏能力 | 内置一键隐藏模块，开机自动部署 |
| 元模块 | 内置 Hybrid Mount，模块挂载更稳 |
| 工具箱 | boot 备份、SystemUI 重启、缓存清理、Evil Service 自查 |
| 内核 | 支持 LKM（GKI 2.0）与内核补丁两种方式 |

## 目录结构

```
kernel/         内核补丁与内核模块
userspace/      ksud 等用户态程序
manager/        Android 管理器 App
docs/           文档
scripts/        构建与工具脚本
```

## 构建

```bash
cd manager
./gradlew :app:assembleRelease
```

内核与 ksud 的构建见 `.github/workflows/` 与 `justfile`。

## 文档

- 安装路线：[docs/INSTALL_ROUTES.md](docs/INSTALL_ROUTES.md)
- 验证清单：[docs/VERIFY.md](docs/VERIFY.md)
- 第三方许可：[docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)

## 贡献者

- [hhcdxii-ship-it](https://github.com/hhcdxii-ship-it) —— 内核 / 管理器 / 发布
- [Yuyan9178345](https://github.com/Yuyan9178345) —— 隐藏模块（[Snow-love-Module](https://github.com/HuijuiSU-NEXT/Snow-love-Module)）

完整名单见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

## 交流

- Telegram 群：https://t.me/Huijui123
- Telegram 频道：https://t.me/HuijuiSU
- QQ 群：763278354

## 许可

见 [LICENSE](LICENSE)。
