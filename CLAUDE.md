# CLAUDE.md

This file provides guidance for Claude Code when working in this repository.

## Project Overview

**Velto** is a lightweight Minecraft core plugin (alpha) targeting Paper/Spigot/Bukkit servers. It is intentionally minimal — no economy, homes, or warps. The goal is a plug-and-play core that plays well with other plugins.

- Current version: `0.7.2-SNAPSHOT`
- Supported MC versions: 1.21.4–1.21.11 (1.21.4 is LTS)
- Java package root: `com.aven0x`

## Module Structure

```
common/   # Shared code (commands, listeners, managers, utils) — targets Bukkit/Spigot API
bukkit/   # Bukkit/Spigot-specific plugin entry point
paper/    # Paper-specific entry point + Paper-only commands (e.g. /anvil)
```

Key classes:
- `common/.../VeltoPlugin.java` — shared plugin bootstrap
- `paper/.../VeltoPaper.java` — Paper entry point
- `paper/.../managers/CommandManager.java` — dynamic command registration
- `common/.../commands/BaseCommand.java` — base class all commands extend

## Build

```bash
# Build the Bukkit jar
./gradlew :bukkit:build

# Build the Paper jar
./gradlew :paper:build
```

Output jars land in `bukkit/build/libs/` and `paper/build/libs/`.

Do NOT run `./gradlew build` at the root — it is intentionally blocked.

## Adding a New Command

1. Create `SomeCommand.java` in `common/src/main/java/com/aven0x/Velto/commands/` extending `BaseCommand`.
2. Register it in the appropriate `CommandManager`.
3. Add the permission node and default message entries to `lang.yml` and `commands.yml`.
4. If Paper-only (requires Paper API), put it under `paper/` instead.

## Configuration Files (runtime, not in repo)

| File | Purpose |
|------|---------|
| `config.yml` | Spawn location, base settings |
| `lang.yml` | All player-facing messages (supports chat/actionbar/title/bossbar) |
| `commands.yml` | Enable/disable commands, override permission nodes |

## Code Style

- Standard Java conventions; no Lombok.
- Keep commands self-contained — avoid adding cross-cutting state.
- Async teleportation via `TeleportManager` (already wired); don't block the main thread.
- Tab completion should be added to every command that accepts a player argument.

## What Velto Intentionally Excludes

Do not add: economy, homes, TPA, warps, kits, claims, towns, or minigame systems. These are left to dedicated plugins by design.
