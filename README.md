# zgrnf — Paper/Spigot server plugin

Companion server plugin for the zgrnf client mod (see the `main` branch).
While a client plays music it gets flight; the volume becomes their flight
speed (100 = maxFlySpeed, 0 = hover in place). Pausing cancels flight.

- Plugin message channel: `zgrnf:flight` (2 bytes: state + volume 0-100)
- Modes: `whitelist` / `blacklist` / `off` (config.yml, hot-reloadable)
- Works on Paper **and Folia** (entity scheduler), Java 17 bytecode,
  compatible with servers from 1.20.x to 26.x.
- `api-version: 1.20`, `folia-supported: true`

## Commands

| Command                              | Permission  |
|--------------------------------------|-------------|
| `/zgrnf reload`                      | `zgrnf.admin` |
| `/zgrnf list`                        | `zgrnf.admin` |
| `/zgrnf flying`                      | `zgrnf.admin` |
| `/zgrnf mode <whitelist\|blacklist\|off>` | `zgrnf.admin` |
| `/zgrnf whitelist add\|remove\|list <玩家>` | `zgrnf.admin` |
| `/zgrnf blacklist add\|remove\|list <玩家>` | `zgrnf.admin` |

## Building

```powershell
$env:JAVA_HOME = 'D:\java\zulu17'
gradle build
```

Output: `build/libs/zgrnf-<version>-all.jar` (shadow jar).

## License

MIT, see [LICENSE](LICENSE).
