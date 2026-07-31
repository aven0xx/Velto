# Folia support & the scheduler SPI

Velto is written to run unchanged on **Spigot**, **Paper**, and **Folia** from a single
codebase. Folia is the hard target: it replaces the single main thread with many
per-region tick loops running in parallel, so any code that touches the wrong thread
throws rather than corrupts. This doc explains how the plugin stays correct across all
three, and how to keep it that way when you extend it.

Read [ARCHITECTURE.md](ARCHITECTURE.md) first for the module split — everything here
builds on why `common` can only see the Spigot API.

## The problem in one paragraph

`common` compiles against `org.spigotmc:spigot-api`, which has **none** of Folia's
region-scheduler types — yet almost every scheduled task in the plugin lives in
`common`. So `common` literally cannot *name* `GlobalRegionScheduler`,
`EntityScheduler`, etc. Bumping `common` to `paper-api` would make the Spigot-only
`bukkit` jar throw `NoClassDefFoundError` at runtime. The fix is a small platform-neutral
**scheduler SPI** defined in `common` and implemented once per module.

## The SPI

Three types in `common/.../platform/`:

| Type | Role |
|---|---|
| `VeltoScheduler` | The platform-neutral scheduling contract. `common` code only ever names this. |
| `VeltoTask` | A cancellable handle — stands in for `BukkitTask` / Folia's `ScheduledTask`. |
| `Schedulers` | A `volatile` static holder: `Schedulers.set(impl)` once in `onEnable`, `Schedulers.get()` everywhere else. |

Each runtime module supplies its own implementation and installs it as the **second line
of `onEnable`** (right after `VeltoPlugin.set(this)`):

| Module | Implementation | Backed by |
|---|---|---|
| `paper` | `FoliaScheduler` | The four Folia region schedulers — present on ordinary Paper too, where they route to the main thread. No reflection, no `isFolia()` branching. |
| `bukkit` | `BukkitSchedulerAdapter` | The classic single-threaded `BukkitScheduler`. Regions collapse to the main thread; the `retired` callback has no analogue and is dropped. |

The result: `common` stays byte-for-byte valid against the Spigot API, every Folia type
stays inside the `paper` module, and there is exactly one choke point for all scheduling.

## The four lanes

`VeltoScheduler` mirrors Folia's execution model. Picking the right lane *is* the whole
job:

| Lane | Method(s) | Use for | Examples in the tree |
|---|---|---|---|
| **global** | `global`, `globalDelayed`, `globalTimer` | State owned by no location: time, weather, game rules, console commands, server-wide broadcasts | `/day` `/night` `/time` `/weather`, kit reward commands, TPA expiry, auto-messages |
| **entity** | `entity`, `entityDelayed`, `entityTimer` | Anything touching a specific entity/player; the task follows them across regions | teleport countdown, `setAfk`, per-player boss bar / action bar, join chat completions |
| **region** | `region` | Block/chunk work anchored at a `Location` | *(none currently — reserved for future block/chunk work)* |
| **async** | `async`, `asyncTimer` | Off-tick work that never touches server state — file I/O | userdata writes + autosave, AFK-position writes, `/baltop` balance scan |

Plus two non-lane methods: `teleport(entity, loc)` (the only supported teleport on Folia —
see below) and `owns(entity|location)` (replaces `Bukkit.isPrimaryThread()`), and
`cancelAll()` for shutdown.

Delays/periods are in ticks except the async lane (milliseconds). Values below `1` are
clamped to `1` on every platform, so the two jars behave identically.

## Region-safety patterns

These are the recurring shapes the rework introduced. When you write new code, reuse
them.

### Touching a player who may not be the sender → `PlayerUtil.onOwningRegion`
```java
PlayerUtil.onOwningRegion(target, () -> {
    target.setHealth(20);            // mutation runs on the region that owns target
    LangUtil.send(target, "healed"); // the target's own message goes inside too
});
LangUtil.send(sender, "healed-other"); // sender feedback stays on the caller's region
```
`onOwningRegion` runs the action **inline** when the caller already owns the target
(always the case on Spigot's single thread, and for self-targeted commands) and only
actually hops on Folia. Used by `/heal` `/feed` `/kill` `/fly` `/speed` `/gamemode`
`/kit <name> <other>` `/sudo`.

### World / console / broadcast state → `global`
```java
Schedulers.get().global(() -> world.setTime(1000)); // message stays outside the hop
```

### Teleporting → never `Player#teleport`
`TeleportManager.teleportAsync` delegates to `Schedulers.get().teleport(...)`, which uses
`Entity#teleportAsync` on the owning region (Folia's only supported path) and the classic
chunk-load-then-teleport on Spigot. Reading a *foreign* player's location as a destination
is itself a cross-region read, so those commands (`/tp <a> <b>`, `/tpaaccept`) read it
inside an `onOwningRegion` hop first.

### Iterating players → `PlayerUtil.onlineSnapshot()`
`Bukkit.getOnlinePlayers()` is a live view whose iteration is undefined off the owning
thread. Always iterate `PlayerUtil.onlineSnapshot()` instead.

### Config caches → immutable snapshot behind one `volatile`
`/veltoreload` rewrites caches while other regions read them. Fields that are read as a
group (e.g. the AFK-zone location) live in one immutable object behind a single `volatile`
reference; map caches (`LangUtil`, `KitManager`) are rebuilt into a fresh map and swapped
in one write, never `clear()`-then-refilled in place. See [ARCHITECTURE.md](ARCHITECTURE.md)
and `ConfigUtil`.

## Platform behaviour, side by side

| | Spigot | Paper (non-Folia) | Folia |
|---|---|---|---|
| Scheduler impl | `BukkitSchedulerAdapter` | `FoliaScheduler` | `FoliaScheduler` |
| `owns(...)` | `isPrimaryThread()` | `isOwnedByCurrentRegion(...)` (true on main) | real region check |
| A hop (`onOwningRegion`, `global`) | runs **inline** | runs inline / next global tick | actually hops to the owning region |
| Net effect | **unchanged from before the rework** | unchanged | correct across regions |

**Bukkit/Spigot compatibility is preserved by construction:** no Paper/Folia (or Adventure)
type appears in `common` or `bukkit`, the sync teleport path is the same code relocated into
`BukkitSchedulerAdapter`, and `owns()` is `isPrimaryThread()` so every hop runs inline on the
single main thread. A grep audit of the branch confirms zero `io.papermc` / `net.kyori` /
`threadedregions` imports in `common` or `bukkit`.

## What's done and what's deferred

Done: the scheduler SPI + migration, teleportation, global-region state, cross-region
entity mutation, the `/killall` redesign, online-player snapshots, and concurrency
hardening. **`folia-supported: true` is set** in `paper-plugin.yml` (the "Partial Folia
support" change), so Folia will load the plugin — treat it as an enable-for-testing
switch, not a certification. The smoke test below is still the gate before relying on
Folia in production, which is what the "Recode"/partial-support version label signals.

Deferred, on purpose:

- **The Adventure migration** (boss bars / titles / action bars → `net.kyori.adventure`)
  is intentionally left as the legacy `net.md_5.bungee` API. Adventure is **not** bundled
  in spigot-api (only paper-api), so moving `LangUtil` to it would break the `bukkit` jar.
  Cost: a few deprecated calls plus a mild, unlikely boss-bar-registry concern on Folia —
  not a blocker.
- **`/killall` on Folia** clears entities within ~8 chunks of the caller (the region it
  runs on) instead of a whole world — there is no consistent cross-region entity list. It
  stays world-wide on Spigot/Paper. Feedback reads "near you" on Folia so the narrowed
  scope is visible.

## Testing on Folia

The failure mode on Folia is loud: a mis-scheduled call throws a
`Thread failed main thread check` (or `UnsupportedOperationException`). So the strategy is
to exercise each lane across **multiple regions** and watch the console.

**Setup:** Folia 1.21.8, at least two players **far apart** — different dimensions is the
surest way to guarantee different regions. A single player in one spot won't exercise
cross-region paths. Keep the console open; after the session grep the log:

```bash
grep -iE "failed main thread check|UnsupportedOperation|asynchronously|not owned by|while disabled" logs/latest.log
```

**Zero hits = schedulers are behaving.** Any hit names the failing feature.

| Lane | Trigger | Passes if… |
|---|---|---|
| global | `/day` `/night` `/time set 6000` `/weather rain` `/sun` `/rain` `/thunder`; a kit with a `commands:` entry; `/baltop` | effect happens, no console error |
| globalDelayed | `/tpa <p>` and let it expire; `/alert bossbar 100 hi` | "tpa-expired" fires after timeout; server-wide bar shows then clears |
| globalTimer | auto-messages (short interval); idle past the AFK timeout; `/alert actionbar 100 hi` | broadcasts on schedule; AFK triggers; global action bar repeats then stops |
| entity | `/heal` `/feed` `/gamemode … <other>` targeting a player in another region | other player is affected; no "not owned by" error |
| entityDelayed | quit **while AFK** then rejoin; join and check `@name` completions; a per-player boss bar (`/notiftest`) | pending-return teleport fires; completions appear; bar shows then clears |
| entityTimer | `/spawn` `/home` `/warp` `/back` with a countdown > 0; a per-player action bar (`/notiftest`) | countdown ticks and teleports; moving mid-countdown cancels it |
| async / asyncTimer | change a balance, play, let autosave run; `/baltop`; then `/stop` | userdata persists; no region-tick lag (`/spark tps`); clean shutdown, no "while disabled" warning |
| teleport + owns | `/tp <a> <b>`, `/tpall` (players in 3 regions), `/tpaaccept` across regions, `/home` into another dimension | everyone teleports; no thread-check error |

**Best single canary:** `/home` with a countdown to a home in the **nether while you stand
in the overworld** hits `entityTimer` + `owns` + `teleport` + `entity` at once across a
dimension boundary. If that's clean, the core of the migration is sound.

Also run the full session on **plain Paper** and **Spigot** as a regression check — the
same jars must behave exactly as before.

## Adding Folia-safe code

Quick rules (see [EXTENDING.md](EXTENDING.md#scheduling--thread-safety-folia) for the
longer version):

- **Never** use `Bukkit.getScheduler()` / `BukkitRunnable` in `common` or `paper`. Schedule
  through `Schedulers.get()`.
- Mutating/reading a **player who may not be the sender** → wrap in
  `PlayerUtil.onOwningRegion(target, ...)`.
- World time/weather/game rules, console commands, server-wide broadcasts → `global(...)`.
- File I/O → `async(...)`. Never touch server state from the async lane.
- Iterating players → `PlayerUtil.onlineSnapshot()`, never the live `getOnlinePlayers()`.
- Teleport via `TeleportManager` only; never call `Player#teleport` directly.
- Any static cache refreshed by `/veltoreload` → publish a fresh immutable value in one
  `volatile` write; don't mutate a shared collection in place.
