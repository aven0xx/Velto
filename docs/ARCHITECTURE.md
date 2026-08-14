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
│       ├── platform/       the scheduler SPI (VeltoScheduler, VeltoTask, Schedulers) — see FOLIA.md
│       └── utils/          stateless/static helpers (LangUtil, ConfigUtil, CommandUtil, ...)
├── bukkit/   Bukkit/Spigot entry point (com.aven0x.VeltoBukkit.VeltoBukkit)
│   └── src/main/java/com/aven0x/VeltoBukkit/
│       ├── managers/CommandManager.java       registers every command
│       ├── managers/ChatManager.java          legacy chat formatting (AsyncPlayerChatEvent)
│       ├── platform/BukkitSchedulerAdapter.java  VeltoScheduler on the classic BukkitScheduler
│       └── utils/DynamicCommandRegistrar.java reflection-based command registration
├── paper/    Paper entry point (com.aven0x.VeltoPaper.VeltoPaper)
│   └── src/main/java/com/aven0x/VeltoPaper/
│       ├── commands/AnvilCommand.java         Paper-only (needs Paper's openAnvil API)
│       ├── managers/CommandManager.java       registers every command (+ anvil)
│       ├── managers/ChatManager.java          Adventure-based chat formatting (AsyncChatEvent)
│       ├── platform/FoliaScheduler.java       VeltoScheduler on Folia's region schedulers
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

## Scheduling: the `VeltoScheduler` SPI

`common` must schedule work (timers, delayed tasks, async I/O) but compiles against the
Spigot API, which has none of Folia's region-scheduler types — and Folia has no main
thread, so `BukkitScheduler` throws there. The fix is a platform-neutral scheduling
interface, `VeltoScheduler` (`common/.../platform/`), that `common` code reaches through
the `Schedulers.get()` static holder. Each module installs its own implementation in
`onEnable` — `FoliaScheduler` on Paper, `BukkitSchedulerAdapter` on Bukkit — so `common`
stays free of any Paper/Folia type and there's one choke point for every scheduling
decision. The same jars then run correctly on Spigot, Paper, and Folia.

**All scheduling in `common` and `paper` goes through `Schedulers.get()` — never
`BukkitScheduler` or `BukkitRunnable`.** The full model (the four lanes, the region-safety
patterns, the Bukkit-compat guarantee, and how to test it) is in [FOLIA.md](FOLIA.md);
read it before adding anything that schedules, teleports, mutates another player, or
iterates the online-player list.

## Plugin lifecycle (`onEnable`)

Both `VeltoPaper.onEnable()` and `VeltoBukkit.onEnable()` follow the same sequence
(they're kept in lock-step manually — there's no shared base class):

1. `VeltoPlugin.set(this)`, immediately followed by `Schedulers.set(...)` (the platform's
   `FoliaScheduler`/`BukkitSchedulerAdapter`) — both must happen before anything else
   touches `VeltoPlugin.get()` or schedules work.
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
`TeleportManager.getInstance().cancelAll()` (cancel pending countdowns),
`UserdataManager.stopAutosave()`, then `Schedulers.cancelAllQuietly()` to stop in-flight
async writers **before** the exclusive `UserdataManager.saveAll()` (synchronous — this is
the one place a blocking save is correct, since the server is shutting down; cancelling
the async writers first means the final flush can't race one). The pre-`saveAll` ordering
matters on Folia, where async tasks aren't halted automatically before `onDisable`.

### `/veltoreload`

`ReloadCommand` (registered as `veltoreload`) re-runs the load step of most of the
above (`ConfigUtil.reload()`, `LangUtil.load()`, `CommandUtil.load()`,
`KitManager.load()`, `WarpManager.reload()`, `EconomyManager.load()` +
`VaultHook.refresh()`, `AutoMsgManager.restart()`), each wrapped in its own
try/catch so one failing section doesn't abort the rest. It does **not** re-register
commands — `commands.yml`'s per-command `enabled`/`aliases` only take effect on the
next full restart, since command registration happens once during `onEnable`.

## Cross-platform compatibility shim

`ServerUtil` (`common/.../utils/ServerUtil.java`) detects the platform at runtime by
probing for marker classes via `Class.forName` (a string, so it adds no compile-time
dependency): `isPaper()` looks for `io.papermc.paper.configuration.Configuration`, and
`isFolia()` looks for `io.papermc.paper.threadedregions.RegionizedServer`. These let
`common` behave differently per platform without naming a Paper/Folia type — `isFolia()`
is what makes `/killall` fall back to a caller-region entity sweep on Folia while staying
world-wide on Spigot/Paper.

Platform-specific *behaviour* that used to live behind `isPaper()` reflection — notably
teleportation — now goes through the [scheduler SPI](#scheduling-the-veltoscheduler-spi)
instead. `TeleportManager.teleportAsync` delegates to `Schedulers.get().teleport(...)`,
which each module implements natively (`Entity#teleportAsync` on Paper/Folia, the classic
chunk-load-then-`teleport` on Spigot); no reflection is involved anymore. See
[FOLIA.md](FOLIA.md) for the whole model.
