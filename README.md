# Velto *(Alpha)*  
![License](https://img.shields.io/github/license/aven0xx/Velto) [![Latest Release](https://img.shields.io/github/v/release/aven0xx/Velto)](https://github.com/aven0xx/Velto/releases/latest)

> **⚠ Alpha Notice:** Velto is a **small, lightweight core plugin** built with the help of AI and refined over time. It was imagined as a **simple, plug-and-play core** that doesn’t need frequent updates, is easy to extend, and works well alongside other major plugins.

**Velto** is not meant to replace big all-in-one solutions like Essentials, CMI, or EternalCore. Instead, it focuses on providing a minimal set of utilities and a configurable message system. It can be used as:
- A **simple core** for standalone servers.
- A **foundation plugin** in a cross-server setup, leaving things like economy, homes, and teleports to dedicated plugins.

📚 **[Read the Velto Wiki →](https://github.com/aven0xx/Velto/wiki)** for installation, configuration, and development guides.

---

## 🚦 Philosophy

- **Keep it light:** Minimal features, no unnecessary systems.
- **Play well with others:** Designed to work with existing plugins without overlap.
- **Easy to set up:** Drop it in, start the server, and it works.
- **Extendable:** Clean architecture so you can add your own features.

---

## 📥 Downloads

Grab builds in **[Releases](../../releases)**:

- **`-paper`** – For Paper servers (includes Paper-only commands).  
- **`-bukkit`** – For Spigot/Bukkit; also works on Paper.

---

## ✅ Included Commands

- `/heal`, `/feed`, `/speed`, `/gamemode`, `/kill`  
- `/time`, `/day`, `/night`, `/weather`, `/itemlore`
- `/craft`, `/broadcast`,  `/killall`, `/rename`
- `/setspawn`, `/spawn` *(Set and teleport to a spawn point)*  
- `/anvil` *(Paper-only because of limitations of Spigot API)*

---

## 🧠 Configurable Messages

Velto’s `lang.yml` lets you choose how messages are displayed:
- **chat**  
- **actionbar**  
- **title**  
- **bossbar**  

Placeholders are supported for dynamic values.

---

## ❌ What Velto Does Not Include

- Economy  
- Homes/TPA/Warps (only `/spawn` is provided)  
- Kits, claims, towns, minigames

These are intentionally left out so you can use the best existing plugins for your needs.

---

## 📂 Configuration Files

```text
config.yml   # spawn location & base settings
lang.yml     # fully configurable messages
commands.yml # enable/disable Velto commands
