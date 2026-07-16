# Guidelines.md

This file provides guidance for Claude Code/ChatGPT Codex when working in this repository.

## RULES

- A session link must never be attached to a commit or a Pull Request made by Claude or ChatGPT Codex
- Aven0xx is always the author of every commit while Claude/ChatGPT Codex is Co-Author
- Always ask before making changes to the codebase to get the permission to do it


## Project Overview

**Velto** is a lightweight Minecraft core plugin (alpha) targeting Paper/Spigot/Bukkit servers. It is intentionally minimal — no claims, towns, or minigame systems (an optional, fully toggleable economy module is the one exception — see "What Velto Intentionally Excludes" below). The goal is a plug-and-play core that plays well with other plugins with every command that can be disabled.

- Current version: `0.7.5-SNAPSHOT`
- Supported MC versions: 1.21.8–26.2 (1.21.8 is LTS, supported until Fall 2026). 1.21.7 and earlier are unsupported/untested.
  - `api-version` in `plugin.yml`/`paper-plugin.yml` is intentionally kept at `1.21.4` — below the supported floor — so the plugin isn't refused by future server versions. This does **not** mean 1.21.4–1.21.7 are officially supported; it's a compatibility floor, not a support commitment.
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

## Configuration Files

Shipped as default resources (`paper/src/main/resources/` and `bukkit/src/main/resources/` — keep both copies in sync) and extracted into the plugin's data folder on first run.

| File | Purpose |
|------|---------|
| `config.yml` | Spawn location, AFK/teleport/TPA/back/chat settings |
| `lang.yml` | All player-facing messages (supports chat/actionbar/title/bossbar) |
| `commands.yml` | Enable/disable commands, override permission nodes/aliases |
| `kits.yml` | Kit definitions (items, cooldowns, commands) |
| `economy.yml` | Economy module toggle, currency settings, Vault toggle |

Full reference: [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md). Runtime-generated
data (userdata, warps, AFK positions) is covered separately in
[`docs/DATA_STORAGE.md`](docs/DATA_STORAGE.md).

## Code Style

- Standard Java conventions; no Lombok.
- Keep commands self-contained — avoid adding cross-cutting state.
- Async teleportation via `TeleportManager` (already wired); don't block the main thread.
- Tab completion should be added to every command that accepts a player argument.

## What Velto Intentionally Excludes

Do not add: claims, towns, or minigame systems. These are left to dedicated plugins by design.

Velto does ship a lightweight, single-currency economy module (`/economy`, `/balance`, `/pay`, with an
optional Vault hook) — but it is fully toggleable via `economy.yml` (`enabled: false`) for server owners
who prefer a dedicated economy plugin.
