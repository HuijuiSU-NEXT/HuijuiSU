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

<p align="center">
  <a href="https://github.com/ShirkNeko" title="ShirkNeko"><img src="https://github.com/ShirkNeko.png?size=64" width="40" height="40" alt="ShirkNeko"></a>
  <a href="https://github.com/tiann" title="tiann"><img src="https://github.com/tiann.png?size=64" width="40" height="40" alt="tiann"></a>
  <a href="https://github.com/AlexLiuDev233" title="AlexLiuDev233"><img src="https://github.com/AlexLiuDev233.png?size=64" width="40" height="40" alt="AlexLiuDev233"></a>
  <a href="https://github.com/5ec1cff" title="5ec1cff"><img src="https://github.com/5ec1cff.png?size=64" width="40" height="40" alt="5ec1cff"></a>
  <a href="https://github.com/Ylarod" title="Ylarod"><img src="https://github.com/Ylarod.png?size=64" width="40" height="40" alt="Ylarod"></a>
  <a href="https://github.com/luyanci" title="luyanci"><img src="https://github.com/luyanci.png?size=64" width="40" height="40" alt="luyanci"></a>
  <a href="https://github.com/liankong233" title="liankong233"><img src="https://github.com/liankong233.png?size=64" width="40" height="40" alt="liankong233"></a>
  <a href="https://github.com/Tools-cx-app" title="Tools-cx-app"><img src="https://github.com/Tools-cx-app.png?size=64" width="40" height="40" alt="Tools-cx-app"></a>
  <a href="https://github.com/aviraxp" title="aviraxp"><img src="https://github.com/aviraxp.png?size=64" width="40" height="40" alt="aviraxp"></a>
  <a href="https://github.com/weblate" title="weblate"><img src="https://github.com/weblate.png?size=64" width="40" height="40" alt="weblate"></a>
  <a href="https://github.com/backslashxx" title="backslashxx"><img src="https://github.com/backslashxx.png?size=64" width="40" height="40" alt="backslashxx"></a>
  <a href="https://github.com/rsuntk" title="rsuntk"><img src="https://github.com/rsuntk.png?size=64" width="40" height="40" alt="rsuntk"></a>
  <a href="https://github.com/igormiguell" title="igormiguell"><img src="https://github.com/igormiguell.png?size=64" width="40" height="40" alt="igormiguell"></a>
  <a href="https://github.com/dabao1955" title="dabao1955"><img src="https://github.com/dabao1955.png?size=64" width="40" height="40" alt="dabao1955"></a>
  <a href="https://github.com/yujincheng08" title="yujincheng08"><img src="https://github.com/yujincheng08.png?size=64" width="40" height="40" alt="yujincheng08"></a>
  <a href="https://github.com/hhcdxii-ship-it" title="hhcdxii-ship-it"><img src="https://github.com/hhcdxii-ship-it.png?size=64" width="40" height="40" alt="hhcdxii-ship-it"></a>
  <a href="https://github.com/WenHao2130" title="WenHao2130"><img src="https://github.com/WenHao2130.png?size=64" width="40" height="40" alt="WenHao2130"></a>
  <a href="https://github.com/hmtheboy154" title="hmtheboy154"><img src="https://github.com/hmtheboy154.png?size=64" width="40" height="40" alt="hmtheboy154"></a>
  <a href="https://github.com/Howard20181" title="Howard20181"><img src="https://github.com/Howard20181.png?size=64" width="40" height="40" alt="Howard20181"></a>
  <a href="https://github.com/MiRinChan" title="MiRinChan"><img src="https://github.com/MiRinChan.png?size=64" width="40" height="40" alt="MiRinChan"></a>
  <a href="https://github.com/symbuzzer" title="symbuzzer"><img src="https://github.com/symbuzzer.png?size=64" width="40" height="40" alt="symbuzzer"></a>
  <a href="https://github.com/RooGhz720" title="RooGhz720"><img src="https://github.com/RooGhz720.png?size=64" width="40" height="40" alt="RooGhz720"></a>
  <a href="https://github.com/TinyHai" title="TinyHai"><img src="https://github.com/TinyHai.png?size=64" width="40" height="40" alt="TinyHai"></a>
  <a href="https://github.com/SaleOff" title="SaleOff"><img src="https://github.com/SaleOff.png?size=64" width="40" height="40" alt="SaleOff"></a>
  <a href="https://github.com/natsumerinchan" title="natsumerinchan"><img src="https://github.com/natsumerinchan.png?size=64" width="40" height="40" alt="natsumerinchan"></a>
  <a href="https://github.com/s1204IT" title="s1204IT"><img src="https://github.com/s1204IT.png?size=64" width="40" height="40" alt="s1204IT"></a>
  <a href="https://github.com/reindex-ot" title="reindex-ot"><img src="https://github.com/reindex-ot.png?size=64" width="40" height="40" alt="reindex-ot"></a>
  <a href="https://github.com/u9521" title="u9521"><img src="https://github.com/u9521.png?size=64" width="40" height="40" alt="u9521"></a>
  <a href="https://github.com/Dr-TSNG" title="Dr-TSNG"><img src="https://github.com/Dr-TSNG.png?size=64" width="40" height="40" alt="Dr-TSNG"></a>
  <a href="https://github.com/YuKongA" title="YuKongA"><img src="https://github.com/YuKongA.png?size=64" width="40" height="40" alt="YuKongA"></a>
  <a href="https://github.com/Prslc" title="Prslc"><img src="https://github.com/Prslc.png?size=64" width="40" height="40" alt="Prslc"></a>
  <a href="https://github.com/whyakari" title="whyakari"><img src="https://github.com/whyakari.png?size=64" width="40" height="40" alt="whyakari"></a>
  <a href="https://github.com/KOWX712" title="KOWX712"><img src="https://github.com/KOWX712.png?size=64" width="40" height="40" alt="KOWX712"></a>
  <a href="https://github.com/Aquarius223" title="Aquarius223"><img src="https://github.com/Aquarius223.png?size=64" width="40" height="40" alt="Aquarius223"></a>
  <a href="https://github.com/rifsxd" title="rifsxd"><img src="https://github.com/rifsxd.png?size=64" width="40" height="40" alt="rifsxd"></a>
  <a href="https://github.com/skbeh" title="skbeh"><img src="https://github.com/skbeh.png?size=64" width="40" height="40" alt="skbeh"></a>
  <a href="https://github.com/resuki-workbot" title="resuki-workbot"><img src="https://github.com/resuki-workbot.png?size=64" width="40" height="40" alt="resuki-workbot"></a>
  <a href="https://github.com/CanerKaraca23" title="CanerKaraca23"><img src="https://github.com/CanerKaraca23.png?size=64" width="40" height="40" alt="CanerKaraca23"></a>
  <a href="https://github.com/Coconutat" title="Coconutat"><img src="https://github.com/Coconutat.png?size=64" width="40" height="40" alt="Coconutat"></a>
  <a href="https://github.com/Hikari-no-Tenshi" title="Hikari-no-Tenshi"><img src="https://github.com/Hikari-no-Tenshi.png?size=64" width="40" height="40" alt="Hikari-no-Tenshi"></a>
  <a href="https://github.com/ygorigor" title="ygorigor"><img src="https://github.com/ygorigor.png?size=64" width="40" height="40" alt="ygorigor"></a>
  <a href="https://github.com/raystef66" title="raystef66"><img src="https://github.com/raystef66.png?size=64" width="40" height="40" alt="raystef66"></a>
  <a href="https://github.com/aaaaaaaa-815" title="aaaaaaaa-815"><img src="https://github.com/aaaaaaaa-815.png?size=64" width="40" height="40" alt="aaaaaaaa-815"></a>
  <a href="https://github.com/ShiinaSaku" title="ShiinaSaku"><img src="https://github.com/ShiinaSaku.png?size=64" width="40" height="40" alt="ShiinaSaku"></a>
  <a href="https://github.com/diphons" title="diphons"><img src="https://github.com/diphons.png?size=64" width="40" height="40" alt="diphons"></a>
</p>

- [hhcdxii-ship-it](https://github.com/hhcdxii-ship-it) —— 内核 / 管理器 / 发布
- [Yuyan9178345](https://github.com/Yuyan9178345) —— 隐藏模块（[Snow-love-Module](https://github.com/HuijuiSU-NEXT/Snow-love-Module)）

完整名单见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

## 交流

- Telegram 群：https://t.me/Huijui123
- Telegram 频道：https://t.me/HuijuiSU
- QQ 群：763278354

## 许可

见 [LICENSE](LICENSE)。
