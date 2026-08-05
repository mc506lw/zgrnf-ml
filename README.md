# Lapis-Zgrnf

Chinese people can fly. Press **G** to open the music player UI.

Multi-loader mod (Fabric + NeoForge) maintained as a single common source set
that builds against multiple Minecraft versions.

## Version branches

One Git branch per Minecraft generation. Switch branches to build a different
generation; inside a branch, the exact Minecraft / loader version is selected
by Gradle properties at build time.

| Branch        | Minecraft      | NeoForge | Fabric       |
|---------------|----------------|----------|--------------|
| `main`        | 26.2 / 26.1    | 26.x     | 26.x         |
| `1.21`        | 1.21.11 / 1.21.8 / 1.21.4 / 1.21.1 | 21.x | 1.21.x |

Branch content diverges in `common/` only where the Minecraft API itself
changed between generations (e.g. rendering model, permission API). Small
intra-generation differences are absorbed at runtime with reflection in
`common/`, so a single branch covers several patch versions.

## Requirements

- Gradle 9.5.1 (`D:\java\gradle-9.5.1\bin\gradle.bat`)
- JDKs configured in `gradle.properties` `org.gradle.java.installations.paths`
  - Minecraft 26.x -> Java 25 (`D:\java\zulu25`)
  - Minecraft 1.21.x -> Java 21 (`D:\java\zulu21`)
- `maven.neoforged.net` is blocked on this network (GFW). The root
  `build.gradle` mirrors NeoForge/NeoForm through the QLU BMCLAPI mirror and a
  hand-curated local repo (`.neoforged-local-repo`) that fills the gaps the
  mirror does not sync (e.g. `net.neoforged.fancymodloader` poms).

## Building

`-Pkey=value` cannot be used on Windows: PowerShell passes `=` to cmd.exe,
which truncates the value (e.g. `-Pminecraft_version=26.1.2` becomes `26`).
Override Gradle properties with environment variables instead:

```powershell
# Minecraft 26.2 (defaults in gradle.properties, JAVA_HOME must be zulu25)
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

Jars land in `neoforge/build/libs/` and `fabric/build/libs/`.

The exact `neoforge_version` / `neo_form_version` / `fabric_version` to use for
a Minecraft version can be looked up on:

- https://projects.neoforged.net/neoforged/neoforge
- https://projects.neoforged.net/neoforged/neoform
- https://fabricmc.net/develop/

## Layout

- `common/`  loader-agnostic code (game logic, UI, networking, music player)
- `neoforge/` NeoForge entry points and `neoforge.mods.toml`
- `fabric/`   Fabric entry points and `fabric.mod.json`
- `build-logic/` shared Gradle conventions for the modules

## License

MIT, see [LICENSE](LICENSE).
