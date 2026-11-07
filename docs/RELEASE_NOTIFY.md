# 发布与通知（Telegram Bot）

## Bot

| 项 | 值 |
|---|---|
| 用户名 | `@huijui_su_bot` |
| 显示名 | HuijuiSU |
| 目标群 | Huijui内测反馈群（`t.me/Huijui123`，supergroup id `-1004497125144`） |
| 权限 | 群成员（建议设为管理员） |

Token 只存在仓库 Secret `BOT_TOKEN` 里，**不要写进任何源码或文档**。
需要轮换时：BotFather → `/revoke` → 新 token → 更新 Secret。

## 自动通知链路

```
git tag v6.x && git push origin refs/tags/v6.x
        │
        ├─ Release 工作流
        │    ├─ build-manager（编译 APK / LKM / ksud）
        │    ├─ release（创建 GitHub Release，上传 5 个 APK）
        │    └─ notify（推送 Telegram）
        │         └─ CHAT_ID = vars.CHAT_ID || '@Huijui123'
        │
        └─ build-manager 的 upload-telegram（APK 直传，>50MB 会跳过）
```

约束：

- Bot API 单文件上限 **50 MB**，`universal` 包 72 MB 传不了，只发下载链接
- `CHAT_ID` 用 `@Huijui123` 或 `-1004497125144` 都行，前提是 bot 在群里
- 没配 `BOT_TOKEN` 时 notify 会打 warning 并正常退出（不报红）

## 手动补推

`notify-telegram.yml` 支持 `workflow_dispatch`，但**必须先在默认分支上**才会出现在 Actions 页面。
目前它在 `feature/huijui-ultra`，需要合到 `main` 才能点按钮。

临时手动推送也可以用：

```bash
curl -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendMessage" \
  -d chat_id=-1004497125144 \
  --data-urlencode "text=..." 
```

## 相关链接

- GitHub：https://github.com/HuijuiSU-NEXT/HuijuiSU
- Releases：https://github.com/HuijuiSU-NEXT/HuijuiSU/releases
- TG 群：https://t.me/Huijui123
- bot：https://t.me/huijui_su_bot
- QQ 群：763278354

## 历史

- 2026-11-07 v6.0 正式版发布，TG 通知首次打通（message_id=8）