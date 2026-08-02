# zgrnf-ml

中国人会飞。按 **G** 打开音乐播放界面,播放音乐即可获得飞行。

跨加载器 mod(Fabric + NeoForge),以单一 common 源码集构建多个 Minecraft 版本。

## 功能

- 播放音乐时玩家获得飞行能力,音量大小即飞行速度(100% 时达到 `maxFlySpeed`)。
- 两种飞行模式:
  - **creative(创造原生)**:像创造模式一样双击空格起飞,速度随音量,手感最稳(默认)。
  - **jetpack(喷气背包)**:双击空格起飞后持续喷气上升,推力随音量增强,可再次双击空格取消飞行降落。
- 可选音符粒子特效:飞行中的玩家身上冒音符粒子(`particles` 开关)。
- 服务端命令 `/zgrnf` 可热重载配置、查看玩家、切换白名单/黑名单/飞行模式/粒子。
- 按 **O** 键切换播放/暂停音乐(客户端)。

## 版本分支

一个 Minecraft 大版本对应一个 Git 分支;分支内通过构建属性精确选择 Minecraft / 加载器版本。

| 分支          | Minecraft            | NeoForge | Fabric |
|---------------|----------------------|----------|--------|
| `main`        | 26.2 / 26.1          | 26.x     | 26.x   |
| `1.21`        | 1.21.11 / 1.21.8 / 1.21.4 / 1.21.1 | 21.x | 1.21.x |

只有 Minecraft API 自身在不同大版本间变化时,`common/` 内容才会在分支间不同(如渲染模型、权限 API)。大版本内的小差异在 `common/` 中运行时用反射吸收,因此一个分支可覆盖多个补丁版本。

## 环境要求

- Gradle 9.5.1(`D:\java\gradle-9.5.1\bin\gradle.bat`)
- JDK 配置在 `gradle.properties` 的 `org.gradle.java.installations.paths`:
  - Minecraft 26.x -> Java 25(`D:\java\zulu25`)
  - Minecraft 1.21.x -> Java 21(`D:\java\zulu21`)
- `maven.neoforged.net` 在本网络被 GFW 阻断。根目录 `build.gradle` 通过 QLU BMCLAPI 镜像 + 手工维护的本地仓库(`.neoforged-local-repo`)补足镜像缺失的部分(如 `net.neoforged.fancymodloader` 的 pom)。

## 构建

Windows 下不能用 `-Pkey=value`:PowerShell 会把 `=` 传给 cmd.exe 导致值被截断(如 `-Pminecraft_version=26.1.2` 变成 `26`)。请用环境变量覆盖 Gradle 属性:

```powershell
# Minecraft 26.2(gradle.properties 默认值,JAVA_HOME 需为 zulu25)
gradle clean :neoforge:build :fabric:build

# Minecraft 26.1.2
$env:JAVA_HOME = 'D:\java\zulu25'
$env:ORG_GRADLE_PROJECT_version = '26.1.0.0'
$env:ORG_GRADLE_PROJECT_minecraft_version = '26.1.2'
$env:ORG_GRADLE_PROJECT_neo_form_version = '26.1.2-1'
$env:ORG_GRADLE_PROJECT_neoforge_version = '26.1.2.93'
$env:ORG_GRADLE_PROJECT_fabric_version = '0.155.2+26.1.2'
$env:ORG_GRADLE_PROJECT_minecraft_version_range = '[26.1, 26.2)'
gradle clean :neoforge:build :fabric:build
```

jar 输出在 `neoforge/build/libs/` 与 `fabric/build/libs/`。

某 Minecraft 版本对应的 `neoforge_version` / `neo_form_version` / `fabric_version` 可在以下地址查询:

- https://projects.neoforged.net/neoforged/neoforge
- https://projects.neoforged.net/neoforged/neoform
- https://fabricmc.net/develop/

## 目录结构

- `common/`  与加载器无关的代码(游戏逻辑、UI、网络、音乐播放)
- `neoforge/` NeoForge 入口与 `neoforge.mods.toml`
- `fabric/`   Fabric 入口与 `fabric.mod.json`
- `build-logic/` 模块共享的 Gradle 约定

## 许可证

MIT,见 [LICENSE](LICENSE)。
