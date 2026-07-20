# Commands

## The `BaseCommand` contract

Every command is a class extending `common/.../commands/BaseCommand.java`:

```java
public abstract class BaseCommand {
    protected final String name;               // canonical registered name, set via super(name)

    public abstract boolean execute(CommandSender sender, String label, String[] args);
    public List<String> complete(CommandSender sender, String label, String[] args) { ... }
    public boolean canUse(CommandSender sender) { ... }

    protected boolean isPlayer(CommandSender sender)             // false + message if console
    protected boolean checkPermission(CommandSender sender, String perm)  // silent check, override-aware
    protected boolean hasPermission(CommandSender sender, String perm)    // checkPermission + "no-permission" message
}
```

- **`execute`** does the work. Return `true` always in this codebase (returning `false`
  would trigger Bukkit's built-in usage-message fallback, which every command here
  avoids by sending its own usage message via `LangUtil` instead).
- **`complete`** returns tab-completion suggestions. Default: empty list.
- **`canUse`** gates whether the command shows up at all (tab-completion, `/help`
  listings) — it should mirror whatever permission `execute` enforces. It's checked
  independently by each platform's registrar (`BasicCommand#canUse` on Paper,
  `Command#testPermissionSilent` on Bukkit) — see [ARCHITECTURE.md](ARCHITECTURE.md).
- **`label`** is whatever alias the player actually typed (not necessarily the
  canonical `name`) — commands use it in usage messages (e.g.
  `KillAllCommand`: `"Usage: /" + label + " <entityType|ALL> [world]"`).
- **Permission checks are deliberately duplicated**: `canUse()` for visibility, then
  `hasPermission()` again as the first thing inside `execute()`. This is intentional
  defense-in-depth, not an oversight — don't "clean up" one of the two checks.
- **Console support is inconsistent by design**, not a bug: commands that only make
  sense for a physical player (`/home`, `/tp`, `/back`, `/pay`, ...) call `isPlayer()`
  first and bail for console. Admin commands that act *on* a player
  (`/kill`, `/feed`, `/god`, `/economy give`, `/sudo`, `/kitreset`, ...) are
  console-usable by design, following the pattern in `KitResetCommand`/`SudoCommand`:
  no `isPlayer()` guard, and every `LangUtil.send(player, ...)` call is paired with an
  `else sender.sendMessage("...")` plain-text fallback for console. When adding a new
  admin-style command, follow that pairing rather than assuming a `Player` sender.

## How a command gets from `/foo` to a `BaseCommand`

1. `CommandManager.registerAllCommands()` (per-platform) calls a private `register(name,
   Supplier<BaseCommand>)` for every command.
2. `register()` first checks `CommandUtil.isEnabled(name)` — if `commands.yml` has
   `<name>.enabled: false`, the command is never constructed or registered at all (not
   even hidden — genuinely absent). This check only runs once, at startup.
3. Otherwise it constructs the command and calls `DynamicCommandRegistrar.registerCommand(name,
   command)`, then again for every alias from `CommandUtil.getAliases(name)` — **the
   same `BaseCommand` instance** is registered under all of its names.
4. The platform-specific registrar wires `execute`/`complete`/`canUse` into whatever
   the server's command API expects (see [ARCHITECTURE.md](ARCHITECTURE.md) for why
   this differs between Paper and Bukkit).

## Permission resolution — the `commands.yml` override layer

`checkPermission(sender, "velto.xxx")` does **not** check `"velto.xxx"` directly — it
calls `CommandUtil.getPermission(this.name, "velto.xxx")`, which looks up
`commands.yml`'s `<command>.permissions.velto.xxx` key and falls back to the literal
`"velto.xxx"` if no override is configured. This lets a server owner remap Velto's
permission nodes to their own scheme without touching code:

```yaml
fly:
  enabled: true
  aliases: []
  permissions:
    velto.fly: myplugin.fly
    velto.fly.others: myplugin.fly.others
```

This indirection applies to the *primary* permission(s) each command checks via
`checkPermission`/`hasPermission`. **Dynamic, unbounded sub-permissions bypass it** —
e.g. `KitCommand`'s per-kit node (`"velto.kit." + kit.name().toLowerCase()`) is checked
with a raw `sender.hasPermission(...)`, since there's no fixed key to override in
`commands.yml` for an admin-defined, open-ended kit list.

All permission nodes are declared (undocumented `default`, i.e. Bukkit's built-in
default of **op-only**) as children of `velto.*` in `bukkit/src/main/resources/plugin.yml`
and `paper/src/main/resources/paper-plugin.yml` — **both files must be updated
together**, they're not generated from one source. Nothing is granted to normal
players by default; every server is expected to wire nodes up through a permissions
plugin (LuckPerms, etc.).

## Full command catalog

"Self/Others" means the command supports both `/cmd` (acting on yourself) and `/cmd
<player>` (acting on someone else) with separate permission nodes — this is the same
pattern in `FeedCommand`, `HealCommand`, `FlyCommand`, `GodCommand`,
`GamemodeCommands`, `SpeedCommand`, and `BalanceCommand`.

| Command | Aliases | Permission(s) | Console | Notes |
|---|---|---|---|---|
| `/spawn` | — | `velto.spawn` | ❌ | Teleports to the configured spawn (`config.yml: spawn`). |
| `/setspawn` | — | `velto.setspawn` | ❌ | Sets spawn to your current location. |
| `/time [set] <day\|night\|noon\|midnight\|0-24h\|1-12am/pm\|ticks> [world]` | — | `velto.timeset` | ✅ | Optional `set` keyword. Real hours use an `h` suffix (`8h`, `18h`) or am/pm (`6pm`); a bare number is raw ticks. |
| `/day [world]` | `setday` | `velto.timeset` | ✅ | |
| `/night [world]` | `setnight` | `velto.timeset` | ✅ | |
| `/weather <clear\|sun\|rain\|thunder> [world]` | — | `velto.weather` | ✅ | |
| `/sun`, `/rain`, `/thunder` `[world]` | — | `velto.weather` | ✅ | Shortcut wrappers that re-dispatch through `/weather`. |
| `/craft` | `workbench` | `velto.craft` | ❌ | Opens a virtual crafting table. |
| `/anvil` | `repair` | `velto.anvil` | ❌ | **Paper only** — uses `Player#openAnvil`, unavailable on Spigot API. |
| `/list` | — | `velto.list` | ❌ | Lists online players. |
| `/notiftest <lang-key>` | — | `velto.notiftest` | ❌ | Debug command: sends yourself a raw `lang.yml` message by key. |
| `/rename <name...>` / `/rename reset` | — | `velto.rename` | ❌ | Renames the item in your main hand. |
| `/itemlore show\|clear\|add\|insert\|set\|remove ...` | — | `velto.lore` | ❌ | Edits lore of the held item. |
| `/feed [player]` | — | `velto.feed` / `velto.feed.others` | ✅* | Self/others. *Console must name a target. |
| `/heal [player]` | — | `velto.heal` / `velto.heal.others` | ✅* | Self/others. |
| `/speed <0-10> [player]` | — | `velto.speed` / `velto.speed.others` | ✅* | Self/others; console must name a target. |
| `/god [player]` | — | `velto.god` / `velto.god.others` | ✅* | Self/others. Toggle; enforced by `GodListener` (cancels damage/hunger/targeting/negative potions). |
| `/fly [player]` | — | `velto.fly` / `velto.fly.others` | ✅* | Self/others toggle of `allowFlight`. |
| `/kill [player]` | `suicide` | `velto.kill` | ✅* | Self/others. |
| `/killall <entityType\|ALL> [world]` | — | `velto.killall` | ❌ | Refuses to target `PLAYER` regardless of permission (hardcoded safety). |
| `/gamemode <mode> [player]` | `gm` | `velto.gamemode.<mode>` / `.<mode>.others` | ✅* | Self/others, permission checked per requested mode. |
| `/gmc`, `/gms`, `/gma`, `/gmsp` `[player]` | `gm1`/`creative`, `gm0`/`survival`, `gm2`/`adventure`, `gm3`/`spectator` | same as above, fixed mode | ✅* | Shortcut single-mode versions of `/gamemode`. |
| `/back` | — | `velto.back` | ❌ | Teleports to your last death/teleport location (`BackListener`); blocked in `config.yml: back.blacklisted-worlds`. |
| `/tp <player>` / `<x> <y> <z>` / `<player> <player>` / `<player> <x> <y> <z>` | — | `velto.tp` / `velto.tp.others` | ❌ | Supports `~` relative coordinates. |
| `/tpall` | — | `velto.tpall` | ❌ | Teleports every online player to you. |
| `/tpa <player>` | — | `velto.tpa` | ❌ | Sends a teleport request; expires after `config.yml: tpa.expire-seconds`. |
| `/tpaaccept [player]` | `tpaccept` | `velto.tpa` | ❌ | Accepts the most recent request, or a named requester's. |
| `/tpadeny [player]` | `tpdeny` | `velto.tpa` | ❌ | Denies likewise. |
| `/sudo <player> <command...>` | — | `velto.sudo` | ✅ | Forces the target to run a command. |
| `/msg <player> <message...>` | `tell`, `w`, `whisper` | `velto.msg` | ❌ | Also reachable via `@player message` in chat (`AtMentionHandler`). |
| `/reply <message...>` | `r` | `velto.msg` | ❌ | Replies to `MsgManager`'s last-messenger record. |
| `/afk` / `/afk <player>` / `/afk list` | `away` | `velto.afk` / `velto.afk.others` / `velto.afk.list` | ❌ | Toggle; auto-triggers after `config.yml: afk-timeout-seconds` of inactivity. |
| `/home [name]` | — | `velto.home` | ❌ | Default name `"home"`. |
| `/sethome [name]` | — | `velto.sethome` (+ `velto.homes.bonus.<name>.<amount>` / `velto.homes.unlimited` for the cap) | ❌ | Capped per-player: `config.yml: homes.default-limit` (3) plus each additive `velto.homes.bonus.<name>.<amount>`, or uncapped via `velto.homes.unlimited`. See [PLACEHOLDERS.md](PLACEHOLDERS.md#home-limits). |
| `/delhome [name]` | — | `velto.delhome` | ❌ | |
| `/homes` | — | `velto.homes` | ❌ | Lists your own homes. |
| `/warp [name]` | — | `velto.warp` | ❌ | No args lists all warps (global, not per-player). |
| `/setwarp <name>` | — | `velto.setwarp` | ❌ | Name is required (warps are shared/named, unlike `/sethome`). |
| `/kit` / `/kit <name> [player]` / `/kit preview <name>` | — | `velto.kit` (+`.others`, `.<kitname>`, `.cooldown.bypass`) | ✅* | See `kits.yml`; `preview` opens a locked read-only inventory. |
| `/kitreset <player> <kit>` | — | `velto.kit.reset` | ✅ | Clears cooldown + one-time-claim flag. |
| `/economy give\|take\|set\|reset <player> [amount]` | `eco` | `velto.economy.give`/`take`/`set`/`reset` | ✅ | Gated live by `economy.yml: enabled`. See [ECONOMY.md](ECONOMY.md). |
| `/balance [player]` | `bal`, `money` | `velto.balance` / `velto.balance.others` | ❌ | Self/others. |
| `/pay <player> <amount>` | — | `velto.pay` | ❌ | Blocks self-pay and insufficient funds. |
| `/alert <chat\|actionbar\|bossbar\|title> <ticks> <message...>` | `broadcast`, `bc` | `velto.alert` | ✅ | Broadcasts a one-off raw message, bypassing `lang.yml`. |
| `/veltoreload` | — | `velto.reload` | ✅ | Reloads every config file — see [ARCHITECTURE.md](ARCHITECTURE.md#veltoreload). |

## Command "families" in one file

A few related commands share a single `.java` file rather than one-class-per-file,
because they share most of their logic:

- **`GamemodeCommands.java`** — `GamemodeCommand` (generic `/gamemode <mode>
  [player]`) plus four `GamemodeShortcutCommand` subclasses (`gmc`/`gms`/`gma`/`gmsp`)
  that hardcode the mode. `GamemodeCommand.canUse()` returns `true` if the sender has
  *any* gamemode permission (so the command is visible/tab-completable even if they
  can only use one mode) — the specific mode's permission is re-checked inside
  `execute()`.
- **`WeatherCommand.java`** — the generic `/weather <mode> [world]` plus `SunCommand`/
  `RainCommand`/`ThunderCommand`, which just re-dispatch to `/weather <mode>` to reuse
  its world-resolution and messaging instead of duplicating it.

If you're adding a small family of closely-related commands, this is the established
pattern — see [EXTENDING.md](EXTENDING.md).
