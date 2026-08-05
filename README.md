# Lapis-Zgrnf — Paper/Spigot/Folia 服务端插件

Lapis-Zgrnf 客户端 mod(见 `main` 分支)的配套服务端插件。客户端播放音乐时获得飞行,音量即飞行速度(100% 时达到 `maxFlySpeed`),暂停音乐即取消飞行。

- 插件消息通道:`zgrnf:flight`(2 字节:状态 + 音量 0-100)
- 放行规则:`whitelist` / `blacklist` / `off`(config.yml,可热重载)
- 支持 Paper **和 Folia**(使用实体调度器),Spigot 自动回退全局调度器
- Java 17 字节码,兼容 1.20.x ~ 26.x 服务端
- `api-version: 1.20`,`folia-supported: true`

## 功能

- **两种飞行模式**(config.yml 的 `flight-mode`):
  - `creative`:创造原生飞行。双击空格起飞,速度随音量,手感最稳(默认)。
  - `jetpack`:喷气背包。双击空格起飞后持续喷气上升,推力随音量增强,再次双击空格取消飞行降落。
- **音符粒子特效**:飞行中的玩家身上冒音符粒子(可选,`particles: true/false`)。
- 创造/旁观模式玩家始终保留原生飞行,暂停或停止时只重置飞行速度,不会误关。

## 命令

| 命令                                        | 权限 |
|---------------------------------------------|------|
| `/zgrnf reload`                             | `zgrnf.admin` |
| `/zgrnf list`                               | `zgrnf.admin` |
| `/zgrnf mode <whitelist\|blacklist\|off>`   | `zgrnf.admin` |
| `/zgrnf flightmode <creative\|jetpack>`     | `zgrnf.admin` |
| `/zgrnf particles <on\|off>`                | `zgrnf.admin` |
| `/zgrnf whitelist add\|remove\|list <玩家>` | `zgrnf.admin` |
| `/zgrnf blacklist add\|remove\|list <玩家>` | `zgrnf.admin` |

## 配置

首次启动会自动生成带中文注释的 `config.yml`,所有选项都有说明,改完执行 `/zgrnf reload` 生效。

## 构建

```powershell
$env:JAVA_HOME = 'D:\java\zulu17'
gradle build
```

输出:`build/libs/Lapis-Zgrnf-<version>-all.jar`(shadow jar)。

## 许可证

MIT,见 [LICENSE](LICENSE)。
