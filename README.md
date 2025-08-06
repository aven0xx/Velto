# Velto

**Velto** is a modern core plugin with basic command for Minecraft server. 
All commands and messages are configurable. Open source plugin and easy to extand.

---

## ✨ Features

- 🚀 Spigot and Paper compatibility 
- ✅ Modern permission checks (`velto.command` / `velto.command.others`)
- 🛠️ Utility commands like:
  - `/heal`, `/feed`, `/speed`, `/gamemode`, `/kill`
  - `/time`, `/day`, `/night`, `/weather`, `/setspawn`, `/spawn`
  - `/craft`, `/anvil`, `/broadcast`
- 🧠 Configurable message system using `lang.yml`
- 🎨 Notification types: `chat`, `actionbar`, `title`, `bossbar` (0.2 and more)
- 🔄 Asynchronous teleportation support
- 🧩 Easy to extend via clean command structure (`BaseCommand`)

---

## 📂 File Structure

```plaintext
config.yml         # Stores spawn point, and other data
lang.yml           # All messages (type + content) are defined here
commands.yml       # Enable or disable commands from Velto
