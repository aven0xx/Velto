# Configuration Files

All of these are **shipped resources** — they live under `paper/src/main/resources/`
and `bukkit/src/main/resources/` (duplicated, one copy per module) and are copied out
to the plugin's data folder on first run via `saveResource(...)` (Bukkit's standard
"extract if missing" behavior). **When you change one, change both copies** — nothing
keeps them in sync automatically. A quick sanity check:

```bash
diff paper/src/main/resources/lang.yml bukkit/src/main/resources/lang.yml
```

should be empty for every file in the table below except `plugin.yml`/`paper-plugin.yml`
(which are structurally different file *formats*, see below) — `config.yml` currently
has one harmless one-line drift (a version-number comment) that predates this
documentation; don't treat that as a template for intentional divergence.

For **runtime-generated** data (userdata, warps, AFK positions) rather than
ship-and-edit config, see [DATA_STORAGE.md](DATA_STORAGE.md) instead.

| File | Loaded by | When |
|---|---|---|
| `config.yml` | `ConfigUtil.refreshCache()` | `onEnable`, `/veltoreload` |
| `lang.yml` | `LangUtil.load()` | `onEnable`, `/veltoreload` |
| `commands.yml` | `CommandUtil.load()` | `onEnable`, `/veltoreload` (registration itself only re-reads at next restart) |
| `kits.yml` | `KitManager.load()` | `onEnable`, `/veltoreload` |
| `economy.yml` | `EconomyManager.load()` | `onEnable`, `/veltoreload` |
| `plugin.yml` (bukkit) / `paper-plugin.yml` (paper) | the server itself, at jar load | plugin load (never re-read at runtime) |

## `config.yml`

| Section | Key(s) | Purpose |
|---|---|---|
| — | `spawn` | Location object for `/spawn`; `null` until `/setspawn` runs. Read/written directly by `ConfigUtil.getSpawn()`/`setSpawn()` — not cached in the `refreshCache()` pass. |
| — | `afk-timeout-seconds` (300) | Inactivity threshold before `AfkManager` marks a player AFK. |
| `afkzone` | `enabled`, `location.{world,x,y,z}` | If enabled, AFK players are teleported here and back. |
| `teleport` | `cancel-on-move` (true), `countdown.default` (5), `countdown.permissions` | `TeleportManager`'s player-initiated teleport countdown (see below); first matching permission in the map wins over the default. |
| `tpa` | `expire-seconds` (60) | How long an unanswered `/tpa` request lives. |
| `back` | `blacklisted-worlds` | Worlds `/back` refuses to return you to. |
| `homes` | `default-limit` (3) | Homes a player may set with no `velto.homes.<n>` permission; `HomeManager.getMaxHomes` raises it per the highest such permission, or `velto.homes.unlimited` removes the cap. Enforced by `/sethome`. |
| `userdata` | `autosave-interval-seconds` (300) | Periodic full-flush interval for `UserdataManager` — see [DATA_STORAGE.md](DATA_STORAGE.md), this is a safety net, not the primary save path. |
| `auto-messages` | `enabled`, `interval-seconds`, `random`, `messages` (list of `lang.yml` keys) | `AutoMsgManager`'s periodic broadcast rotation. |
| `messages` | `chat`, `join`, `quit`, `reload`, `chat-priority`, `chat-groups.<name>.{permission,format}` | Chat formatting — consumed by the platform-specific `ChatManager`, not `LangUtil`. `chat-priority` lists group names in priority order; the first group whose `permission` the sender has wins; `chat` is the required fallback format. |

Some of these are cached into `volatile` static fields on `ConfigUtil` at
`refreshCache()` time (fast, lock-free reads from any thread) rather than re-read from
the `FileConfiguration` on every access — if you add a new config key, follow that
pattern (add a `cached*` field, populate it in `refreshCache()`, expose a getter)
rather than reading `getRawConfig()` from hot paths.

## `lang.yml`

Every player-facing message is a top-level key. `LangUtil.load()` parses the whole
file once into a `ParsedMessage` cache (color codes pre-translated, `BaseComponent[]`
pre-built where possible) so sending a message at runtime is just a cache lookup, not
repeated YAML parsing/color translation.

```yaml
some-message-key:
  type: chat            # chat | actionbar | title | bossbar
  message: "&aHello &f%player%&a!"
  duration: 60           # ticks; actionbar/bossbar/title only
  color: GREEN           # bossbar only (BarColor enum name)
  subtitle: "..."        # title only
  hover: "&7tooltip"      # chat only — adds a hover tooltip
  click:                  # chat only — adds a click action
    action: suggest_command   # run_command | suggest_command | open_url | copy_to_clipboard
    value: "/sethome "
```

**Segments** (multiple independently-styled, independently-clickable spans in one chat
line — used for usage messages with clickable examples) replace `message` with a list:

```yaml
rename-usage:
  type: chat
  segments:
    - text: "&eUsage: "
    - text: "&f/rename <name...>"
      hover: "&7Click to start renaming"
      click: { action: suggest_command, value: "/rename " }
```

Placeholders are plain `%token%` substrings, substituted via `String#replace` (not
regex) — pass them as a `Map<String, String>` to `LangUtil.send(player, key, map)`.
`&`-codes are translated to `§` at load time when the message contains no
placeholders (so translation only happens once); messages *with* placeholders are
re-colorized per-send after substitution, since the placeholder value itself might
need color translation.

`LangUtil.sendGlobal(key, ...)` broadcasts to all online players. `LangUtil.sendGlobalRaw(message,
type, durationTicks)` bypasses `lang.yml` entirely for one-off dynamic content
(`/alert` uses this).

## `commands.yml`

Per-command block:

```yaml
<command-name>:
  enabled: true      # if false, the command is never constructed or registered
  aliases: []         # additional registered names, same BaseCommand instance
  permissions:        # optional — overrides checkPermission()'s default node
    velto.xxx: myplugin.xxx
```

`<command-name>` is the canonical name passed to `super(name)` in the `BaseCommand`
subclass — not an alias. See [COMMANDS.md](COMMANDS.md) for the full permission
override mechanism and the complete command list.

## `kits.yml`

Admin-authored, read-only at runtime (players never write to it — cooldowns/claims go
to per-player userdata instead, see [DATA_STORAGE.md](DATA_STORAGE.md)):

```yaml
kits:
  <kit-name>:
    cooldown: 3600          # seconds; 0 = no cooldown; ignored if one-time
    one-time: false          # once claimed, never again (regardless of cooldown) until /kitreset
    commands:                 # optional, run as console per claimer
      - "effect give %player% minecraft:speed 60 1"
    items:
      - material: DIAMOND_SWORD   # required, Bukkit Material enum name
        amount: 1                  # 1-64, default 1
        name: "&bVIP Sword"        # optional, & color codes
        lore: ["&7Line one"]        # optional
        enchantments:                # optional, Minecraft enchantment keys (sharpness, unbreaking, ...)
          sharpness: 3
```

Unknown materials/enchantments are skipped with a warning (`KitManager.LoadResult`
tracks how many), not a hard load failure — a typo in one kit doesn't take down the
others. `velto.kit.<kitname>` (lowercased) is required per-kit, in addition to the
base `velto.kit` permission.

## `economy.yml`

See [ECONOMY.md](ECONOMY.md) for the full module writeup; the config shape:

```yaml
enabled: true              # master switch for the whole economy module
currency:
  name-singular: "Dollar"
  name-plural: "Dollars"
  symbol: "$"
  decimal-places: 2
starting-balance: 0.0
vault:
  enabled: true             # only takes effect if the Vault plugin is actually installed
```

## `plugin.yml` (Bukkit) / `paper-plugin.yml` (Paper)

These are two **structurally different** manifest formats (Paper's supports a native
`dependencies.server` block with `load`/`required`; Bukkit's uses the classic
`softdepend` list), so they can't be a single shared file — but their `permissions:`
trees should always list the exact same nodes. The one intentional platform
difference is `velto.anvil` (paper-plugin.yml only, since `/anvil` doesn't exist on
Bukkit).

Every node lives under `velto.*` as a `children` map (all `true`, meaning granting
`velto.*` grants everything) — no node sets an explicit `default`, so Bukkit's
built-in default of **op-only** applies uniformly. There is deliberately no "give
players basic commands by default" tier; every deployment is expected to wire
permissions through a permissions plugin.

`Vault` and `PlaceholderAPI` are both declared as **soft** dependencies (`softdepend`
on Bukkit; `required: false` on Paper) — Velto works without either installed, but if
present, loads after them so its optional integrations
(`PlaceholderManager.registerExpansion()`, `VaultHook.refresh()`) find them already
enabled. See [EXTENDING.md](EXTENDING.md#adding-an-optional-soft-dependency) if you're
adding another one.
