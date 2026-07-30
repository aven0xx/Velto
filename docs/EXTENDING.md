# Extending Velto

Practical guides for the changes you're most likely to make. Read
[ARCHITECTURE.md](ARCHITECTURE.md) first if you haven't — everything here assumes you
know why `common`/`bukkit`/`paper` are split the way they are.

Before touching anything: read the root [`Guidelines.md`](../Guidelines.md). It has
binding rules for this repo (commit authorship, no session links, **ask before making
changes**) that apply regardless of what these docs describe as technically possible.

## Adding a new command

Worked precedent: `WarpCommand`/`SetWarpCommand` (simple, two commands + one manager)
and `EconomyCommand`/`BalanceCommand`/`PayCommand` (a whole module — see below for
that heavier case).

1. **Create the class** in `common/src/main/java/com/aven0x/Velto/commands/`,
   extending `BaseCommand`:
   ```java
   public class FooCommand extends BaseCommand {
       public FooCommand() { super("foo"); }

       @Override
       public boolean canUse(CommandSender sender) {
           return checkPermission(sender, "velto.foo");
       }

       @Override
       public boolean execute(CommandSender sender, String label, String[] args) {
           if (!isPlayer(sender)) return true;      // omit this if console should be able to run it
           if (!hasPermission(sender, "velto.foo")) return true;
           // ... do the thing, message via LangUtil ...
           return true;
       }

       @Override
       public List<String> complete(CommandSender sender, String label, String[] args) {
           return List.of();  // tab completions, if any
       }
   }
   ```
   If it needs a Paper-only API (like `AnvilCommand`'s `Player#openAnvil`), put it in
   `paper/src/main/java/com/aven0x/VeltoPaper/commands/` instead — it'll only ever be
   registered by Paper's `CommandManager`.
2. **Register it** in `paper/.../managers/CommandManager.java` **and**
   `bukkit/.../managers/CommandManager.java` (unless it's Paper-only):
   ```java
   register("foo", FooCommand::new);
   ```
   Both files use a wildcard import (`com.aven0x.Velto.commands.*`), so a new class in
   `common/commands` resolves automatically — no import line needed.
3. **Add it to `commands.yml`** in both `paper/src/main/resources/` and
   `bukkit/src/main/resources/`:
   ```yaml
   foo:
     enabled: true
     aliases: []
   ```
4. **Add its messages to `lang.yml`** in both resource folders — see
   [CONFIGURATION.md](CONFIGURATION.md#langyml) for the message format.
5. **Add its permission node(s)** to `bukkit/src/main/resources/plugin.yml` **and**
   `paper/src/main/resources/paper-plugin.yml`, as children of `velto.*`:
   ```yaml
   velto.foo: true
   ```
6. **Manually review, since you likely can't compile-check.** This sandbox's network
   policy blocks the Paper/Spigot Maven repos, so a real `./gradlew :paper:build` may
   not be runnable in an agent session — re-read the diff carefully, check permission
   node names match exactly between the command class, `commands.yml`, and both
   plugin manifests, and validate every YAML file parses (a quick `python3 -c "import
   yaml; yaml.safe_load(open('...'))"` per file catches structural mistakes even
   without a real build). Flag to whoever picks this up that a real build/CI pass is
   still needed before merging.

**Console support:** default to requiring `isPlayer()` unless the command's whole
point is to act *on* another player from the console (admin commands like `/kill`,
`/economy give`, `/kitreset`, `/sudo`). If you do support console, every
`LangUtil.send(player, ...)` needs a paired `else sender.sendMessage("...")` — see
`KitResetCommand`/`SudoCommand` for the exact pattern, and
[COMMANDS.md](COMMANDS.md#the-basecommand-contract) for why this is deliberate, not
inconsistent.

## Adding a new manager / feature module

First decide where the data lives — this is the actual design decision, everything
else is mechanical:

| Shape | Use | Precedent |
|---|---|---|
| One value, shared server-wide | Add a key to `config.yml`, read/write via `ConfigUtil` | `spawn`, `afkzone` |
| A named collection, shared server-wide, written rarely | A dedicated manager owning its own YAML file, synchronous save-on-write | `WarpManager` → `warps.yml` |
| A named collection, shared server-wide, written often | Same shape, but **async** save (snapshot + `runTaskAsynchronously`) | `AfkPositionStorage` → `afkposition.yml` |
| Per-player data | `UserdataManager`, namespaced under your own top-level key | `HomeManager` (`homes.*`), `EconomyManager` (`economy.balance`), `KitManager` (`kit-cooldowns.*`) |
| Admin-authored definitions, read-only at runtime | Your own YAML file, loaded once (+ on `/veltoreload`), no write path | `KitManager` reading `kits.yml` |

Full checklist for a manager-backed feature (using `WarpManager` and `EconomyManager`
as the simple/complex reference points, both documented in detail in
[DATA_STORAGE.md](DATA_STORAGE.md) and [ECONOMY.md](ECONOMY.md)):

1. **Manager class** in `common/.../managers/` — follow whichever storage shape you
   picked above. If it owns its own config file, give it a `load()`/`init()` and,
   separately, a `reload()` if external edits should be picked up by `/veltoreload`
   without losing in-memory state made through your own commands (`WarpManager`
   re-reads from disk in `reload()` for exactly this reason).
2. **If it needs its own config file**, create it under both
   `paper/src/main/resources/` and `bukkit/src/main/resources/`, and load it with
   `saveResource(name, false)` if missing (see `KitManager.load()` /
   `EconomyManager.load()` for the pattern) — **unless** it's meant to be pure runtime
   state with no shipped template, in which case follow `AfkPositionStorage`'s
   `file.createNewFile()` approach instead and don't bother committing an empty
   resource copy (there's already one stray, unused example of that mistake —
   `afkposition.yml` under `src/main/resources` — documented in
   [DATA_STORAGE.md](DATA_STORAGE.md), don't repeat it).
3. **Wire init/load into both `onEnable()`s** (`VeltoPaper.java`,
   `VeltoBukkit.java`), in the same relative order as the existing calls (config
   loading before manager construction before command registration) — see
   [ARCHITECTURE.md](ARCHITECTURE.md#plugin-lifecycle-onenable).
4. **Add a reload step to `ReloadCommand`** if the feature has config or file state
   that should refresh without a restart — wrap it in its own `try`/`catch` block,
   matching the existing per-section pattern (one failing section shouldn't abort the
   rest of `/veltoreload`).
5. **Consider a module-level enable toggle** if the feature is substantial enough to
   want fully opt-out (like economy) — check it in every relevant command's
   `canUse()` *and* the top of `execute()`, so it responds live to
   `/veltoreload` rather than requiring a restart like `commands.yml`'s per-command
   toggle does. See [ECONOMY.md](ECONOMY.md#config-economyyml) for the exact pattern.
6. **Commands, permissions, lang, docs** — same steps as
   [Adding a new command](#adding-a-new-command), for each command the feature adds.

If the feature would be economy-, claims-, towns-, or minigame-shaped, re-read
`Guidelines.md`'s exclusion list first and get explicit sign-off before building it —
the economy module is the sole precedent for an exception, and only because it's
fully toggleable.

## Adding an optional soft-dependency

Precedent: `PlaceholderManager` (PlaceholderAPI) and `VaultHook`/`VaultEconomyProvider`
(Vault) — see [ECONOMY.md](ECONOMY.md#vault-integration) for the full class-loading
safety explanation.

1. **Add the dependency as `compileOnly`** in all three `build.gradle` files
   (`common`, `bukkit`, `paper`) — `compileOnly` scope doesn't propagate transitively,
   so each module needs its own declaration even though only `common` might reference
   the classes directly. Add the Maven repo it comes from (per-module `repositories`
   block) if it's not already covered by the root `allprojects` block.
2. **Isolate every reference to the optional plugin's classes inside a dedicated
   class** (or a narrowly-scoped method), and **guard every entry point into that
   class with `Bukkit.getPluginManager().isPluginEnabled("PluginName")`** before the
   guarded code ever instantiates a class implementing/extending one of the optional
   plugin's types, or touches a `.class` literal of one. The JVM resolves those
   symbolic references lazily, at first actual use — so as long as the guard runs
   first, servers without the optional plugin never hit a `NoClassDefFoundError`. Read
   [ECONOMY.md](ECONOMY.md#why-its-safe-to-compile-against-vault-without-requiring-it)
   for the precise mechanics before deviating from this pattern.
3. **Add it as a soft dependency** in both manifests: `softdepend: [...]` in
   `plugin.yml`, `dependencies.server.<Name> { load: BEFORE, required: false }` in
   `paper-plugin.yml` — this ensures it's already enabled by the time Velto's
   `onEnable` checks for it.
4. **Call the registration/init from `onEnable`** (after whatever config controls it
   is loaded) and, if it should respond to config changes without a restart, from
   `ReloadCommand` too.

## Adding a new config file

Follow `kits.yml`/`economy.yml`: create the resource under both platforms'
`src/main/resources/`, load it in your manager with the "extract via `saveResource` if
missing, then `YamlConfiguration.loadConfiguration(file)`" pattern, cache whatever
values are read often into fields rather than re-parsing YAML on every access (see
`ConfigUtil.refreshCache()` / `EconomyManager.load()`), and add a reload step to
`ReloadCommand`. See [CONFIGURATION.md](CONFIGURATION.md) for what's already there and
[DATA_STORAGE.md](DATA_STORAGE.md) if the file is going to be *written* at runtime
rather than only admin-edited.

## Checklist summary

For a plain new command:

- [ ] `BaseCommand` subclass in `common/commands` (or `paper/commands` if Paper-only)
- [ ] Registered in both `CommandManager`s (or just Paper's)
- [ ] Entry in both `commands.yml`
- [ ] Messages in both `lang.yml`
- [ ] Permission node(s) in both `plugin.yml` / `paper-plugin.yml`
- [ ] Console-support decision made deliberately, not by omission
- [ ] Manually reviewed for compile-correctness (imports, method signatures) since a
      real build may not be runnable in-session

For a new manager-backed feature, all of the above per command, plus:

- [ ] Storage shape decided (config value / global file / per-player userdata /
      read-only admin file) and manager written to match
- [ ] Init/load wired into both `onEnable()`s in the right order
- [ ] Reload step added to `ReloadCommand`, own `try`/`catch`
- [ ] Module-level enable toggle, if warranted, checked live in `canUse()` + `execute()`
- [ ] `docs/` updated — this file, plus whichever of `MANAGERS.md`/`DATA_STORAGE.md`/
      a dedicated deep-dive doc fits
