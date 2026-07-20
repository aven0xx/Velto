# Velto *(Alpha)*

[![License](https://img.shields.io/github/license/aven0xx/Velto)](https://github.com/aven0xx/Velto/blob/main/LICENSE) [![Latest Release](https://img.shields.io/github/v/release/aven0xx/Velto)](https://github.com/aven0xx/Velto/releases/latest)

> **⚠ Alpha Notice:** Velto is a **small, lightweight core plugin** built with the help of AI and refined over time. It's a **simple, plug-and-play core** that doesn't need frequent updates, is easy to extend, and works well alongside other major plugins.

**Velto** isn't meant to replace big all-in-one solutions like Essentials, CMI, or EternalCore. It focuses on a solid set of everyday utilities, teleportation, homes/warps, kits, an optional single-currency economy, and a fully configurable message system — with **every command and module individually toggleable**. Use it as:

- A **simple core** for a standalone server.
- A **foundation plugin** in a larger setup, dropping in only the pieces you need and disabling the rest.

📚 **[Velto Wiki →](https://github.com/aven0xx/Velto/wiki)** for installation & configuration guides · **[`docs/`](docs/README.md)** for how the plugin is actually built, if you want to read the code or extend it.

---

## 🚦 Philosophy

- **Keep it light:** a focused utility set, not a kitchen-sink all-in-one.
- **Play well with others:** built to coexist with the plugins you already run, not to compete with them.
- **Everything is toggleable:** every command and every module — economy included — can be switched off independently.
- **Easy to set up:** drop the jar in, start the server, it works with sensible defaults.
- **Extendable:** clean, documented architecture (see [`docs/`](docs/README.md)) so you — or an AI coding agent — can safely add your own features.

---

## ✨ Features

| | |
|---|---|
| **Teleportation** | `/spawn`, `/home`, `/warp`, `/tp`, `/tpa`, `/tpall`, `/back` — with a configurable teleport countdown and cancel-on-move. |
| **Homes & Warps** | Per-player homes (`/home`, `/sethome`, `/delhome`, `/homes`) and shared, admin-set warps (`/warp`, `/setwarp`). |
| **Kits** | Admin-defined kits (`kits.yml`) with cooldowns, one-time claims, and a locked preview GUI. |
| **Economy** *(optional)* | A single-currency economy — `/economy`, `/balance`, `/pay` — backed by per-player balances, with an optional [Vault](https://github.com/MilkBowl/Vault) hook. Fully toggleable; see [below](#-economy-optional). |
| **Player utilities** | `/heal`, `/feed`, `/speed`, `/fly`, `/god`, `/gamemode`, `/kill`, `/killall`, item renaming & lore editing. |
| **World tools** | `/time`, `/day`/`/night`, `/weather` (+ `/sun`/`/rain`/`/thunder`), `/craft`, `/anvil` *(Paper-only)*. |
| **Communication** | `/msg`/`/reply`/`@player` mentions, server-wide `/alert`, per-group chat formats, PlaceholderAPI support. |
| **AFK system** | Auto-detects inactivity, optional AFK teleport zone, survives a quit-while-AFK/rejoin cycle. |
| **Configurable notifications** | Every message can render as chat, an action bar, a title, or a boss bar — see [Configurable Messages](#-configurable-messages). |

---

## 📜 Commands

A quick reference, grouped by category. Every command's exact permission node and
aliases are in **[`docs/COMMANDS.md`](docs/COMMANDS.md)**.

**Teleportation & locations**
`/spawn` `/setspawn` `/home` `/sethome` `/delhome` `/homes` `/warp` `/setwarp`
`/tp` `/tpa` `/tpaaccept` `/tpadeny` `/tpall` `/back`

**Player utilities**
`/heal` `/feed` `/speed` `/fly` `/god` `/gamemode` (`/gmc` `/gms` `/gma` `/gmsp`)
`/kill` `/killall` `/rename` `/itemlore`

**World & environment**
`/time` `/day` `/night` `/weather` (`/sun` `/rain` `/thunder`) `/craft` `/anvil`*

**Communication**
`/msg` `/reply` `@player <message>` `/alert`

**Kits**
`/kit` `/kitreset`

**Economy** *(optional — see below)*
`/economy` `/balance` `/pay`

**Admin & misc**
`/afk` `/sudo` `/list` `/veltoreload` `/notiftest`

<sub>* `/anvil` is Paper-only — Spigot's API doesn't expose the anvil GUI Velto needs.</sub>

Every command can be individually enabled/disabled and given a custom permission
node/aliases through `commands.yml` — nothing here is all-or-nothing.

---

## 💰 Economy *(optional)*

Velto ships a lightweight, single-currency economy, **on by default but fully
toggleable** in `economy.yml`:

- `/economy give|take|set|reset <player> [amount]` — admin balance management.
- `/balance [player]` and `/pay <player> <amount>` — for players.
- Configurable currency name/symbol/decimal places.
- Balances are stored per-player and update live, whether the change came from
  Velto's own commands or from another plugin.
- **Vault integration**, independently toggleable: when enabled and the
  [Vault](https://github.com/MilkBowl/Vault) plugin is installed, Velto registers
  itself as the server's economy provider so shops/quests/other plugins can read and
  modify balances through it. Turn Vault support off if you'd rather run a dedicated
  economy plugin through Vault instead — Velto's own commands keep working either
  way.

Full internals (balance semantics, Vault hook mechanics) are documented in
[`docs/ECONOMY.md`](docs/ECONOMY.md).

---

## 🧠 Configurable Messages

Every player-facing message in `lang.yml` can be delivered as:

- **chat** — including clickable/hoverable segments for usage hints
- **actionbar**
- **title**
- **bossbar**

with `%placeholder%` substitution for dynamic values (player names, amounts, etc.).
See [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md#langyml) for the full message
format.

---

## 🗂️ Version Support

| Minecraft Version | Status |
|:-----------------:|:------:|
| 1.21.7 and less | ❌ Unsupported |
| 1.21.8 | 🟢 LTS (until Fall 2026) |
| 1.21.9 / 1.21.10 | ✅ Supported |
| 1.21.11 | ✅ Supported |
| 26.1 / 26.2 | ✅ Supported |

> **LTS** (Long-Term Support) versions receive priority bug fixes and compatibility updates.
> Unsupported versions may work but are not tested or officially maintained.

---

## 📥 Downloads

Grab builds from **[Releases](../../releases)**:

- **`-paper`** — for Paper servers (includes Paper-only commands like `/anvil`).
- **`-bukkit`** — for Spigot/Bukkit; also runs on Paper (without the Paper-only commands).

---

## 📂 Configuration Files

Shipped, hand-editable config (regenerated in the plugin's data folder on first run):

```text
config.yml    # spawn location, AFK/teleport/TPA/back/chat settings
lang.yml      # every player-facing message, fully configurable
commands.yml  # enable/disable commands, override aliases & permissions
kits.yml      # kit definitions (items, cooldowns, commands)
economy.yml   # economy module toggle, currency settings, Vault toggle
```

Runtime data the plugin generates itself (per-player homes/balances/kit-cooldowns,
warps, AFK return-points) lives alongside these but isn't meant to be hand-edited —
see [`docs/DATA_STORAGE.md`](docs/DATA_STORAGE.md) if you're curious how it's
persisted.

---

## 🚫 What Velto Intentionally Excludes

Claims, towns, and minigame systems — these are left to dedicated plugins by design,
to keep Velto a core rather than an all-in-one. The economy module is the one
deliberate exception, specifically because it's fully toggleable.

---

## 📚 Documentation

The **[`docs/`](docs/README.md)** folder is a full technical reference to how Velto is
built — architecture, the complete command/permission catalog, every config file,
the data persistence model, and practical guides for extending the plugin. It's
written for contributors and for AI coding agents working in this repo alike.

---

## 🤝 Contributing

See [`Guidelines.md`](Guidelines.md) for the project's contribution rules, and
[`docs/EXTENDING.md`](docs/EXTENDING.md) for how to actually add a command, a
manager-backed feature, or an optional integration.
