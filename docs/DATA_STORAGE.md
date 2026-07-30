# Data Storage & Persistence

This covers data the plugin **generates and mutates at runtime** (player balances,
homes, warps, AFK return-points) — as opposed to config you ship and hand-edit, which
is [CONFIGURATION.md](CONFIGURATION.md). All of it is plain YAML on disk, no database.

## Per-player data: `UserdataManager`

**File layout:** `plugins/Velto/userdata/<player-uuid>.yml`, one file per player who
has ever joined. This is the backing store for homes, kit cooldowns/claims, and
economy balances — anything keyed by a single player rather than shared globally.

**Lifecycle:**

1. `UserdataListener.onPreLogin` calls `UserdataManager.load(uuid)` — reads the file
   (or gets an empty `YamlConfiguration` if it doesn't exist yet) into an in-memory
   cache (`Map<UUID, YamlConfiguration>`), *before* the player is even let onto the
   server.
2. While the player is online, every read/write (`getData`, `get`, `set`, ...) hits
   that cached in-memory object directly — no disk I/O on the read/write path itself.
3. `UserdataListener.onQuit` calls `UserdataManager.unload(uuid)`, which saves once
   more and evicts the cache entry.
4. `getData(uuid)` also works for a UUID that was never explicitly `load()`ed (e.g. an
   offline player looked up by a Vault call) — it lazily loads via
   `computeIfAbsent`. That cache entry then has **no corresponding quit event**, so it
   stays resident until server restart. Harmless for occasional offline lookups;
   worth knowing if something starts hammering lookups for many offline players (a
   shop plugin scanning balances, say) — it'll grow that cache for the session.

**Save timing — the part that's easy to get wrong:**

- `UserdataManager.save(uuid)` snapshots the current in-memory `YamlConfiguration` and
  immediately enqueues an **async** disk write (`runTaskAsynchronously`) — it does not
  wait for any interval. Every mutating manager method (`HomeManager.setHome`,
  `EconomyManager.setBalance`, `KitManager.setCooldown`, ...) calls `save()` itself
  right after mutating, so changes reach disk within roughly a tick, not up to 5
  minutes later.
- `UserdataManager.startAutosave(intervalTicks)` (interval from `config.yml`:
  `userdata.autosave-interval-seconds`, default 300s/5min) is a **redundant safety
  net** that re-saves *every currently-loaded player's* data on a timer, regardless of
  whether anything changed since the last explicit `save()`. It is not the primary
  persistence path for anything that already calls `save()` itself.
- `saveAll()` runs synchronously on `onDisable` (the one place a blocking save is
  correct — the server isn't ticking anymore, so there's no thread for an async task
  to run on).

**Why async-per-change instead of always-synchronous?** A synchronous disk write on
every `/pay` or balance change would block the main thread on I/O for every such
action — with enough concurrent players that's exactly the kind of thing that causes
server-wide tick lag. Async-immediate gives durability that's effectively instant
(sub-second in practice) without ever stalling the main thread; the only honest
trade-off is a small window — sub-second — where a change is live in memory but not
yet flushed, so a hard crash in that exact window could in theory lose that one
change. The periodic autosave doesn't change this trade-off; it's just a backstop for
anything else in userdata.

**Concurrency:** all reads/writes on a given player's `YamlConfiguration` are wrapped
in `synchronized (yaml) { ... }` inside `UserdataManager`, since `save()`'s snapshot
step and command-thread mutations can interleave.

**What's stored under each player's file** (namespaced by key prefix — nothing
collides because each feature owns its own top-level key):

| Key prefix | Owner | Contents |
|---|---|---|
| `homes.<name>` | `HomeManager` | `world`, `x`, `y`, `z`, `yaw`, `pitch` |
| `economy.balance` | `EconomyManager` | single `double` |
| `kit-cooldowns.<kitname>` | `KitManager` | last-claimed timestamp (ms) |
| `kit-claimed.<kitname>` | `KitManager` | boolean, for `one-time` kits |
| `ignored` | `IgnoreManager` | list of ignored-player UUID strings |

If you're adding a new **per-player** feature, this is the store to use — see
[EXTENDING.md](EXTENDING.md).

## Global (non-per-player) data

Not everything is per-player. Two features need a single shared file instead, and
each took a different, deliberate approach:

### `WarpManager` → `warps.yml`

Warps are named, shared, and written rarely (an admin runs `/setwarp`). `WarpManager`
keeps one `YamlConfiguration` in memory for the whole plugin (loaded once in `init()`),
mutates it in place, and calls `data.save(file)` **synchronously** right after every
write. This is simpler than `UserdataManager`'s async-queue machinery and is fine
specifically *because* warp writes are infrequent admin actions, not something that
happens every tick under player load — don't copy this synchronous-save pattern for
something that writes often. `WarpManager.reload()` re-reads the file from disk (so
external edits are picked up by `/veltoreload`, not just in-memory changes made
through `/setwarp`).

### `AfkPositionStorage` → `afkposition.yml`

Holds the "teleport back here" location for a player who quit *while* AFK (so the
return can still happen after they rejoin, even though `AfkManager`'s live state was
already cleared on quit). Same shape as `WarpManager` — one in-memory
`Map<UUID, Location>`, loaded once — but saves **async** (`runTaskAsynchronously`,
snapshotting the map on the calling thread first), because this can be written on
every AFK transition for every player, which is a much hotter path than `/setwarp`.

> **Note:** `afkposition.yml` also exists as a checked-in file under
> `paper/src/main/resources/` and `bukkit/src/main/resources/` — but it's empty and
> `AfkPositionStorage.init()` never calls `saveResource()` for it (it just
> `file.createNewFile()`s directly in the data folder if missing). The two committed
> copies aren't functioning as shipped templates the way `kits.yml`/`economy.yml` are;
> the real, live file only ever exists under the plugin's data folder at runtime.

### `spawn` / AFK zone → `config.yml`

The simplest case: a single `Location` value that's part of the main config rather
than its own file, read/written directly via `ConfigUtil.getSpawn()`/`setSpawn()` and
`getAfkzone()`/`setAfkzone()`. Appropriate when there's exactly one value, not a named
collection — see [EXTENDING.md](EXTENDING.md) for the decision between this,
`WarpManager`'s approach, and `UserdataManager`.

## In-memory-only state (never persisted)

Not everything needs to survive a restart. These managers hold pure runtime state in
`ConcurrentHashMap`/`Set` and intentionally have no backing file — a restart clears
them (arguably correctly, since e.g. a stale TPA request or AFK flag shouldn't survive
a reboot):

- `AfkManager` — last-activity timestamps, current AFK set, pre-AFK locations (except
  the quit-while-AFK case above, which *is* persisted).
- `BackManager` — last location per player.
- `GodManager` — the god-mode player set.
- `MsgManager` — last-messenger pairs for `/reply`.
- `TpaManager` — pending teleport requests (with their own expiry timers).

All five expose a `cleanup(uuid)` called from `UserdataListener.onQuit` (or their own
listener) to avoid leaking entries for players who've left.
