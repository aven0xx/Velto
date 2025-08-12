# Velto *(Alpha)*

> **⚠ Alpha Notice:** Velto is a **small core plugin** coded with the help of AI and improved over time for production use. While functional, some features are still missing or under development. Expect updates and refinements as the plugin evolves.

**Velto** is a lightweight, modern core plugin for Minecraft servers, offering essential utility commands with fully configurable messages.  
Designed for **Spigot** and **Paper** servers, it provides a clean, extendable command structure, making it easy to add more features as your server grows.

---

##  Wiki
Detailed setup guidance, documentation, and development info are available on the **[Velto Wiki](https://github.com/aven0xx/Velto/wiki)**.

---

## ​ Features

- 🚀 **Spigot & Paper compatibility** (with some Paper-exclusive features)  
- ✅ **Modern permission checks** (`velto.command` / `velto.command.others`)  
- 🛠️ **Utility commands**, including:
  - `/heal`, `/feed`, `/speed`, `/gamemode`, `/kill`
  - `/time`, `/day`, `/night`, `/weather`, `/setspawn`, `/spawn`
  - `/craft`, `/broadcast`
  - `/anvil` *(Paper-only feature)*  
- 🧠 **Configurable message system** via `lang.yml`  
- 🎨 **Notification types**: `chat`, `actionbar`, `title`, `bossbar` *(0.2+ and beyond)*  
- 🔄 **Asynchronous teleportation support** for smoother player movement  
- 🧩 **Clean & extendable architecture** via `BaseCommand`

---

## ​ File Structure

```plaintext
config.yml         # Stores spawn point and other persistent data
lang.yml           # All messages (type + content) are defined here
commands.yml       # Enable or disable commands in Velto
