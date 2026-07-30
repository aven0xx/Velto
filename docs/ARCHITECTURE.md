# Architecture

## Module layout

Velto is a 3-module Gradle build (`settings.gradle`: `common`, `bukkit`, `paper`),
producing two independent, standalone plugin jars. There is no "core jar + platform
add-on" runtime relationship — `bukkit` and `paper` each bundle their own copy of
`common`'s compiled classes via Shadow (`shadowJar`, `mergeServiceFiles()`). A server
installs exactly one of the two jars.

```
Velto/
├── common/   shared code — targets the (lowest common denominator) Bukkit/Spigot API
│   └── src/main/java/com/aven0x/Velto/
│       ├── commands/       nearly every /command (extends BaseCommand)
│       ├── managers/       stateful singletons (TeleportManager, UserdataManager, EconomyManager, ...)
│       ├── listeners/      Bukkit event listeners
│       ├── integrations/   optional soft-dependency hooks (Vault)
│       └── utils/          stateless/static helpers (LangUtil, ConfigUtil, CommandUtil, ...)
├── bukkit/   Bukkit/Spigot entry point (com.aven0x.VeltoBukkit.VeltoBukkit)
│   └── src/main/java/com/aven0x/VeltoBukkit/
│       ├── managers/CommandManager.java       registers every command
│       ├── managers/ChatManager.java          legacy chat formatting (AsyncPlayerChatEvent)
│       └── utils/DynamicCommandRegistrar.java reflection-based command registration
├── paper/    Paper entry point (com.aven0x.VeltoPaper.VeltoPaper)
│   └── src/main/java/com/aven0x/VeltoPaper/
│       ├── commands/AnvilCommand.java         Paper-only (needs Paper's openAnvil API)
│       ├── managers/CommandManager.java       registers every command (+ anvil)
│       ├── managers/ChatManager.java          Adventure-based chat formatting (AsyncChatEvent)
│       └── utils/DynamicCommandRegistrar.java Brigadier-based command registration
├── build.gradle           root: shared repos, blocks the root `build` task on purpose
├── settings.gradle        declares the 3 modules
└── docs/                  you are here
```

Both `bukkit/build.gradle` and `paper/build.gradle` declare `implementation
project(':common')`. Each module (including `common` itself) separately declares its
own `compileOnly` dependencies (Spigot/Paper API, PlaceholderAPI, VaultAPI) — Gradle's
`compileOnly` scope is not transitive, so a class in `paper` that references a
PlaceholderAPI/Vault type needs its own `compileOnly` entry even though `common`
already declared one. This is why you'll see near-identical dependency blocks repeated
across all three `build.gradle` files.

**Building:** `./gradlew :bukkit:build` or `./gradlew :paper:build`. The root `build`
task is deliberately overridden to throw (`build.gradle:14-18`) so `./gradlew build`
can't silently build things in the wrong order or produce a jar nobody asked for.
Output jars land in `bukkit/build/libs/` / `paper/build/libs/`.

## Why split commands/managers into `common` but duplicate registration?

Bukkit (pre-Paper) and Paper now have genuinely different, non-interoperable APIs for
registering commands dynamically at runtime:

- **Paper** (1.19+) ships a native Brigadier-based lifecycle API
  (`io.papermc.paper.command.brigadier`). `VeltoPaper` hooks
  `LifecycleEvents.COMMANDS` and registers each command as a `BasicCommand`. No
  reflection involved.
- **Bukkit/Spigot** has no equivalent public API for registering commands that aren't
  declared in `plugin.yml` ahead of time. `VeltoBukkit`'s `DynamicCommandRegistrar`
  reaches into the server's private `commandMap` field via reflection
  (`Field.setAccessible(true)`) and registers a raw `BukkitCommand` for each one. This
  is the standard (if inelegant) way plugins have done this on Bukkit for years.

Because these two mechanisms are unrelated, each platform module owns its own
`CommandManager` (which just calls `register(name, Ctor::new)` for every command) and
its own `DynamicCommandRegistrar`. The actual command *logic* (`BaseCommand`
subclasses) lives once in `common` and is handed to whichever registrar is active —
see [COMMANDS.md](COMMANDS.md) for how a `BaseCommand` is registration-agnostic
(it only needs `execute`/`complete`/`canUse`, which both registrars call identically).

The same split exists for chat handling: Paper's `ChatManager` listens for the modern
`AsyncChatEvent` and formats messages as Adventure `Component`s (via
`LegacyComponentSerializer`); Bukkit's `ChatManager` listens for the legacy
`AsyncPlayerChatEvent` and formats plain colored strings. Both classes are otherwise
near-identical (same `resolveChatFormat`/PlaceholderAPI-hook/join-quit-message logic,
duplicated rather than shared because the event types and message types aren't
interchangeable).

`AnvilCommand` is Paper-only for the same reason: it uses `Player#openAnvil`, an API
Spigot doesn't expose. `VeltoBukkit`'s `CommandManager` simply never registers it.

## `VeltoPlugin`: the shared plugin handle

`common`'s classes need a `JavaPlugin` instance (for `getDataFolder()`,
`getLogger()`, the scheduler, etc.) but can't statically depend on `VeltoPaper` or
`VeltoBukkit`. `VeltoPlugin` (`common/.../VeltoPlugin.java`) is a static holder:
whichever platform's `onEnable()` runs calls `VeltoPlugin.set(this)` as its **very
first line**, and every manager/util in `common` calls `VeltoPlugin.get()` instead of
holding its own plugin reference.

## Plugin lifecycle (`onEnable`)

Both `VeltoPaper.onEnable()` and `VeltoBukkit.onEnable()` follow the same sequence
(they're kept in lock-step manually — there's no shared base class):

1. `VeltoPlugin.set(this)` — must happen before anything else touches `VeltoPlugin.get()`.
2. `saveDefaultConfig()` + `ConfigUtil.refreshCache()` — load `config.yml`, cache its values.
3. Load the other shipped config files: `LangUtil.load()`, `CommandUtil.load()`,
   `KitManager.load()`, `WarpManager.init(getDataFolder())`, `EconomyManager.load()`,
   `VaultHook.refresh()`.
4. Construct core managers: `new TeleportManager()`, `new AutoMsgManager()` (+`.start()`),
   `new ChatManager(this)` (self-registers its listeners).
5. Register commands — platform-specific (Brigadier lifecycle event on Paper;
   immediate `CommandManager.registerAllCommands()` call on Bukkit, since there's no
   later lifecycle event to hook).
6. Register the remaining listeners (`GodListener`, `BackListener`, `ChatListener`,
   `KitPreviewListener`, `UserdataListener`).
7. `AfkManager` construction + listener registration, `PlaceholderManager.init()`,
   `AfkManager.start()`.
8. `AfkPositionStorage.init(getDataFolder())`, `UserdataManager.init(getDataFolder())`
   + `UserdataManager.startAutosave(...)`.

The ordering matters in a few places: `ConfigUtil.refreshCache()` must run before
anything reads a cached config value (nearly everything does, transitively);
`WarpManager`/`EconomyManager`/`UserdataManager` must be initialized before any
command can touch them (commands are only *registered* here, not invoked, so this is
naturally safe); soft-dependency hooks (`VaultHook.refresh()`,
`PlaceholderManager.init()`) run after their own config is loaded so they read
up-to-date settings.

### `onDisable`

Much shorter: `AfkManager.stop()`, `AutoMsgManager.stop()`,
`UserdataManager.stopAutosave()` + `UserdataManager.saveAll()` (synchronous — this is
the one place a blocking save is correct, since the server is shutting down and there
won't be another tick for an async task to run on).

### `/veltoreload`

`ReloadCommand` (registered as `veltoreload`) re-runs the load step of most of the
above (`ConfigUtil.reload()`, `LangUtil.load()`, `CommandUtil.load()`,
`KitManager.load()`, `WarpManager.reload()`, `EconomyManager.load()` +
`VaultHook.refresh()`, `AutoMsgManager.restart()`), each wrapped in its own
try/catch so one failing section doesn't abort the rest. It does **not** re-register
commands — `commands.yml`'s per-command `enabled`/`aliases` only take effect on the
next full restart, since command registration happens once during `onEnable`.

## Cross-platform compatibility shim

`ServerUtil.isPaper()` (`common/.../utils/ServerUtil.java`) detects Paper at runtime by
probing for `io.papermc.paper.configuration.Configuration` via `Class.forName`. This
is used inside shared `common` code (e.g. `TeleportManager` prefers Paper's
`teleportAsync` when available, falling back to a synchronous chunk-load + teleport on
plain Spigot) — it's how `common` code can behave slightly differently on the two
platforms without a hard compile-time dependency on Paper's API.
