# Managers, Utils & Listeners

Catalog of every non-command class in `common`, grouped by role. Everything here is a
static-singleton or self-registering class — there's no dependency-injection
framework, classes just call each other's static methods or `VeltoPlugin.get()`
directly. "Persisted?" points to [DATA_STORAGE.md](DATA_STORAGE.md) for detail.

## Core infrastructure

| Class | Role |
|---|---|
| `VeltoPlugin` | Static holder for the active `JavaPlugin` instance. Every other `common` class calls `VeltoPlugin.get()` instead of holding its own plugin reference — see [ARCHITECTURE.md](ARCHITECTURE.md). |
| `ConfigUtil` | Loads/caches `config.yml` into `volatile` static fields (`refreshCache()`); owns `spawn` and `afkzone` location read/write directly (uncached, since they change rarely and are read even more rarely). |
| `LangUtil` | Loads/parses `lang.yml` into a pre-built message cache; `send`/`sendGlobal`/`sendGlobalRaw` are the only way commands should message players — see [CONFIGURATION.md](CONFIGURATION.md#langyml). |
| `CommandUtil` | Loads `commands.yml`; answers `isEnabled(name)`, `getAliases(name)`, `getPermission(name, defaultPerm)` — the permission-override indirection every `BaseCommand.checkPermission` goes through. |
| `TeleportManager` | The **only** path any code should use to move a player. Two modes: `teleport(player, loc[, onComplete])` (player-initiated — honours the countdown/cancel-on-move config; used by `/home`, `/warp`, `/spawn`, `/back`, `/tpaaccept`) and `teleportAsync(player, loc)` (system/admin-initiated — bypasses the countdown entirely; used by `/tp`, `/tpall`, and `AfkManager`'s zone entry/exit). Prefers Paper's native `teleportAsync` via reflection when available, falls back to a synchronous chunk-load + `Player#teleport` on plain Spigot. |
| `UserdataManager` | Per-player YAML cache + save queue. See [DATA_STORAGE.md](DATA_STORAGE.md) for the full lifecycle/timing model — read that before adding a new per-player-persisted feature. |

## Player state managers (in-memory, per-player)

All five below are pure `ConcurrentHashMap`/`Set`-backed, not persisted (see
[DATA_STORAGE.md](DATA_STORAGE.md#in-memory-only-state-never-persisted)), and expose a
`cleanup(uuid)` wired to player-quit.

| Class | Owns | Used by |
|---|---|---|
| `AfkManager` | AFK set, last-activity timestamps, pre-AFK locations. Also a `Listener` itself (move/chat/command/interact/inventory-click events update activity; join/quit handle the persisted quit-while-AFK case via `AfkPositionStorage`). | `AfkCommand` |
| `BackManager` | Last location per player, plus a `backing` flag used to avoid `/back` overwriting itself when `BackListener` sees the resulting teleport. | `BackCommand`, `BackListener` |
| `GodManager` | The god-mode player set (pure toggle, no cooldown/duration logic). | `GodCommand`, `GodListener` |
| `MsgManager` | `recipient → last sender` map, for `/reply`. | `MsgCommand`, `ReplyCommand`, `AtMentionHandler` |
| `TpaManager` | Outgoing request per player (one at a time — sending a new one cancels the old), reverse index for incoming requests, each with its own expiry `BukkitTask`. | `TpaCommand`, `TpaAcceptCommand`, `TpaDenyCommand` |

## Feature managers (persisted)

| Class | Storage | Role |
|---|---|---|
| `HomeManager` | `UserdataManager` (`homes.<name>`) | Set/get/delete/list a player's named homes. Also computes the per-player home cap from `velto.homes.<n>` / `velto.homes.unlimited` permissions (`getMaxHomes`, default `DEFAULT_MAX_HOMES`), enforced by `/sethome` — see [PLACEHOLDERS.md](PLACEHOLDERS.md#home-limits). |
| `WarpManager` | own file, `warps.yml` | Global named warps — see [DATA_STORAGE.md](DATA_STORAGE.md#warpmanager--warpsyml). |
| `KitManager` | `kits.yml` (definitions, read-only) + `UserdataManager` (cooldowns/claims) | Parses kit definitions at load time (skipping invalid materials/enchantments with a warning rather than failing the whole file); builds `ItemStack`s and preview inventories; tracks per-player cooldown/one-time-claim state. |
| `KitPreviewHolder` | — | Marker `InventoryHolder` so `KitPreviewListener` can identify (and lock) a preview GUI without comparing titles. |
| `EconomyManager` | own file, `economy.yml` (config) + `UserdataManager` (`economy.balance`) | See [ECONOMY.md](ECONOMY.md) for the full writeup. |
| `AfkPositionStorage` | own file, `afkposition.yml` | Quit-while-AFK return locations — see [DATA_STORAGE.md](DATA_STORAGE.md#afkpositionstorage--afkpositionyml). |

## Messaging / chat

| Class | Role |
|---|---|
| `AutoMsgManager` | Periodic broadcast rotation (random or sequential) from `config.yml: auto-messages`, driven by a repeating `BukkitTask`. Instance-based (not static) so `/veltoreload` can `restart()` it cleanly. |
| `PlaceholderManager` | Registers `%velto_*%` placeholders with PlaceholderAPI *if installed* (`registerExpansion()` checks `isPluginEnabled("PlaceholderAPI")` before ever touching a PAPI class — see [EXTENDING.md](EXTENDING.md#adding-an-optional-soft-dependency) for why this ordering matters). Holds its own registry (`registerPlaceholder`/`unregisterPlaceholder`) so other managers can contribute placeholders without depending on PAPI directly. See [PLACEHOLDERS.md](PLACEHOLDERS.md) for the full `%velto_*%` catalog. |
| `AtMentionHandler` | Parses `@player message` chat input into a private message; shared by both platforms' `ChatManager`s and by `MsgCommand`'s underlying logic. |
| `ChatManager` (platform-specific: `paper`/`bukkit`) | Chat formatting, join/quit messages, PlaceholderAPI resolution. Duplicated per-platform because Paper's `AsyncChatEvent`/Adventure `Component` and Bukkit's `AsyncPlayerChatEvent`/plain `String` aren't interchangeable — see [ARCHITECTURE.md](ARCHITECTURE.md). |

## Listeners (`common/.../listeners/`)

| Class | Events | Delegates to |
|---|---|---|
| `UserdataListener` | `AsyncPlayerPreLoginEvent`, `PlayerJoinEvent`, `PlayerQuitEvent` | Drives `UserdataManager` load/unload and calls `cleanup()` on `TeleportManager`, `TpaManager`, `MsgManager`, `BackManager`, `GodManager`. This is the central "player left, tear down their runtime state" hook — wire a new per-player in-memory manager's `cleanup()` in here. |
| `BackListener` | `PlayerDeathEvent`, `PlayerTeleportEvent` (both `MONITOR`, `ignoreCancelled`) | `BackManager.saveLocation` / the `backing` self-suppression flag. |
| `GodListener` | `EntityDamageEvent`, `FoodLevelChangeEvent`, `EntityTargetLivingEntityEvent`, `EntityPotionEffectEvent` | Cancels each for players in `GodManager`'s set (potion effects: only negative ones are blocked). |
| `KitPreviewListener` | `InventoryClickEvent`, `InventoryDragEvent` | Cancels interaction with any inventory whose holder is a `KitPreviewHolder`. |
| `ChatListener` | `PlayerJoinEvent`/`PlayerQuitEvent` (custom chat-completion registration for `@name` tab-completing), `TabCompleteEvent` (`@` mention completions), `PlayerQuitEvent` | Purely client-side tab-completion UX for `@mentions` — not to be confused with the platform `ChatManager`s, which handle the actual message formatting. |

## Platform utils (`common/.../utils/`)

| Class | Role |
|---|---|
| `ServerUtil` | `isPaper()` — runtime Paper detection via `Class.forName` probing, used where shared `common` code needs to branch without a compile-time Paper dependency. |
| `PlayerUtil` | `isVanished(player)` — metadata-based check compatible with EssentialsX/SuperVanish-style vanish plugins. Used to suppress AFK/chat broadcasts for vanished players. |
| `DynamicCommandRegistrar` (platform-specific: `paper`/`bukkit`) | Wires a `BaseCommand` into the server's actual command dispatch — Brigadier on Paper, reflection into `CommandMap` on Bukkit. See [ARCHITECTURE.md](ARCHITECTURE.md). |

## Integrations (`common/.../integrations/`)

| Class | Role |
|---|---|
| `VaultHook` | Registers/unregisters `VaultEconomyProvider` with Bukkit's `ServicesManager`, only when Vault is installed **and** `economy.yml`'s toggles allow it. See [ECONOMY.md](ECONOMY.md). |
| `VaultEconomyProvider` | Full implementation of Vault's `Economy` interface (43 methods), delegating every balance operation to `EconomyManager`. |
