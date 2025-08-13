# Velto *(Alpha)*
![License](https://img.shields.io/github/license/aven0xx/Velto) [![Latest Release](https://img.shields.io/github/v/release/aven0xx/Velto)](https://github.com/aven0xx/Velto/releases/latest)

> **⚠ Alpha Notice:** Velto is a **small, minimal core plugin** built with the help of AI and refined over time. It is designed to slot into **multi-server / proxy setups** (Bungee/Waterfall/Velocity) without stepping on other plugins’ toes. Some features are still missing or under development.

**Velto** provides a clean foundation of server utilities and a flexible message system—**without** bundling gameplay systems you already run network-wide (economy, homes, tpa, etc.). Use Velto as the glue around your existing stack.

---

## 🚦 Philosophy

- **Cross-server friendly:** Plays nicely with network/proxy topologies.
- **Do one thing well:** Core utilities & messaging; **no economy**, **no home/tpa**, **no kits**.
- **Composable:** Expect you already run dedicated cross-server plugins (e.g., economy, homes/tpa, permissions).
- **Predictable:** Minimal defaults, clear permissions, everything user-configurable via `lang.yml`.

---

## 📥 Downloads

Grab builds in **[Releases](../../releases)**:

- **`-paper`** – Recommended for Paper servers (enables Paper-only enhancements).
- **`-bukkit`** – For Spigot/Bukkit; runs on Paper as well.

> The Bukkit build should work on Paper, but the Paper build is preferred.

---

## ✅ What Velto Includes (and Excludes)

### Included
- 🔧 **Utility commands** (small, essential set):
  - `/heal`, `/feed`, `/speed`, `/gamemode`, `/kill`
  - `/time`, `/day`, `/night`, `/weather`
  - `/craft`, `/broadcast`
  - `/setspawn`, **`/spawn`** *(the only teleportation command)*
  - `/anvil` *(Paper-only)*
- 🧠 **Configurable messages** via `lang.yml` (chat, actionbar, title, bossbar).
- 🧩 **Extensible architecture** (clean command base; easy to add features).
- 🧵 **Async-friendly** internals where it matters.

### Explicitly Not Included
- ❌ **Economy** — use a dedicated cross-server economy (Vault-compatible) plugin.
- ❌ **Homes/TPA/Warps** — by design, **only `/spawn`** is provided. Expect a network-wide homes/tpa plugin.
- ❌ **Kits/Claims/Towns/Minigames** — out of scope for a minimal core.

> Velto is the **“no overlap”** core: it won’t fight your network standards.

---

## 🔌 Cross-Server Use

Velto is tested with:
- **Paper** on proxied networks (Velocity/Waterfall)
- **Spigot/Bukkit** in single-server or proxied setups

Because Velto avoids economy and player-state systems, it **doesn’t require** database replication or proxy messaging to “stay consistent” with other servers—making multi-server maintenance simpler.

---

## ✨ Feature Highlights

- 🚀 **Spigot & Paper compatibility** (with selective Paper-only commands like `/anvil`)
- 🛡️ **Simple permission model:**
  - Base: `velto.<command>`
  - Others/sudo variants (when applicable): `velto.<command>.others`
- 🎨 **Message pipeline** via `lang.yml`:
  - `type: chat | actionbar | title | bossbar`
  - Placeholder passthrough for your own variables

---

## 📚 Wiki

Docs, setup, and development notes live on the **[Velto Wiki](https://github.com/aven0xx/Velto/wiki)**.

---

## ⚙️ Configuration Files

```text
config.yml   # spawn location & base settings
lang.yml     # fully configurable messages (type + content)
commands.yml # enable/disable Velto commands