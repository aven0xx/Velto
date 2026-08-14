# Velto Developer Documentation

This folder documents how Velto is actually built, so anyone (human or AI agent) can
safely read, extend, or debug the plugin without having to reverse-engineer it from
scratch. It complements — not replaces — the root [`Guidelines.md`](../Guidelines.md),
which holds the *rules* for working in this repo (authorship, permission-to-change
policy, project scope). Read `Guidelines.md` first if you're about to make changes;
read these docs to understand *how* the code works.

## Map

| Doc | What's in it |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module layout (`common`/`bukkit`/`paper`), plugin lifecycle, Paper vs Bukkit differences, build system |
| [FOLIA.md](FOLIA.md) | Folia support: the `VeltoScheduler` SPI, the four scheduler lanes, region-safety patterns, what's deferred, and how to test on Folia |
| [COMMANDS.md](COMMANDS.md) | The `BaseCommand` contract, the full command catalog, and how permissions/aliases/enable-toggles are resolved |
| [CONFIGURATION.md](CONFIGURATION.md) | Every shipped YAML file (`config.yml`, `lang.yml`, `commands.yml`, `kits.yml`, `economy.yml`, `plugin.yml`) explained field by field |
| [DATA_STORAGE.md](DATA_STORAGE.md) | Runtime/generated data: per-player userdata, warps, AFK positions — caching, save timing, persistence guarantees |
| [MANAGERS.md](MANAGERS.md) | Catalog of every manager/util/listener class: what it owns, its public API, whether it's persisted |
| [PLACEHOLDERS.md](PLACEHOLDERS.md) | The full `%velto_*%` PlaceholderAPI catalog and how to add more |
| [ECONOMY.md](ECONOMY.md) | Deep dive on the economy module and its optional Vault integration |
| [EXTENDING.md](EXTENDING.md) | Practical how-to guides: add a command, add a manager/module, add an optional soft-dependency, add a config file |

## Orientation, by what you're trying to do

- **"I want to understand the codebase before touching anything"** → start with
  [ARCHITECTURE.md](ARCHITECTURE.md), then [MANAGERS.md](MANAGERS.md).
- **"I want to add a new `/command`"** → [EXTENDING.md](EXTENDING.md#adding-a-new-command),
  cross-reference [COMMANDS.md](COMMANDS.md) for the conventions existing commands follow.
- **"I want to know what permission node gates X"** → [COMMANDS.md](COMMANDS.md) has the
  full table.
- **"I want to know how balances / homes / warps are actually stored and when they hit
  disk"** → [DATA_STORAGE.md](DATA_STORAGE.md).
- **"I'm working on the economy module or Vault"** → [ECONOMY.md](ECONOMY.md).
- **"I'm adding code that schedules, teleports, or touches another player — will it work on
  Folia?"** → [FOLIA.md](FOLIA.md), then
  [EXTENDING.md](EXTENDING.md#scheduling--thread-safety-folia).
- **"I want to add a whole new feature module (own config file, own manager, maybe an
  optional integration)"** → [EXTENDING.md](EXTENDING.md), using Warp (simple) and
  Economy (complex) as worked examples.

## 30-second orientation

Velto is a multi-module Gradle project:

```
common/   shared code — nearly everything (commands, managers, listeners, utils) lives here
bukkit/   Bukkit/Spigot entry point — reflection-based command registration
paper/    Paper entry point — Brigadier-based command registration + Paper-only commands
```

Both `bukkit` and `paper` depend on `common` and produce their own standalone shaded
jar. A server runs **one or the other**, never both. Almost every behavioral class
(every command, every manager) is written once in `common` and shared; only the plumbing
that differs between the two server APIs (command registration, chat events) is
duplicated per-platform. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full picture.
