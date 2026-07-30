# Economy Module

A single-currency economy: `/economy` (admin), `/balance`, `/pay`, plus an optional
Vault hook — entirely toggleable, because `Guidelines.md` intentionally excludes
economy/claims/towns/minigames from Velto's core scope *unless* fully opt-out-able.
This module is the one exception, specifically because it can be switched off
completely via `economy.yml`.

## Config (`economy.yml`)

```yaml
enabled: true              # master switch for the whole module
currency:
  name-singular: "Dollar"
  name-plural: "Dollars"
  symbol: "$"
  decimal-places: 2
starting-balance: 0.0
vault:
  enabled: true             # only takes effect if the Vault plugin is actually installed
```

`EconomyManager.load()` reads this into `volatile` static fields (same pattern as
`ConfigUtil`). `enabled` is checked **live**, in every economy command's `canUse()`
*and* again at the top of `execute()` — so flipping it off and running
`/veltoreload` hides and disables `/economy`, `/balance`, and `/pay` immediately,
without a server restart (contrast with `commands.yml`'s per-command `enabled`, which
only takes effect on the next restart — see [ARCHITECTURE.md](ARCHITECTURE.md#veltoreload)).

## Balance storage

Balances live in each player's userdata file, key `economy.balance` (a single
`double`), read/written through `EconomyManager` exactly like every other
`UserdataManager`-backed feature — see [DATA_STORAGE.md](DATA_STORAGE.md) for the full
caching/save-timing model. There is **no separate economy database or cache**; every
balance read anywhere in the codebase (a command, a placeholder, a Vault call from
another plugin) goes through the same `EconomyManager.getBalance(uuid)` →
`UserdataManager.getData(uuid)` path, so nothing can drift out of sync between, say, a
Vault-driven change and what `/balance` reports.

## `EconomyManager`'s mutation API — why there are three "subtract" shapes

```java
setBalance(uuid, amount)        // clamps to >= 0, saves immediately
add(uuid, amount)                // deposit — /economy give, Vault depositPlayer
subtract(uuid, amount)           // forced admin subtraction — clamps to 0, always succeeds
withdraw(uuid, amount) -> bool   // strict — fails (no mutation) if balance < amount
transfer(from, to, amount) -> bool  // withdraw(from) then add(to), atomic w.r.t. failure
```

This split exists because "remove money" means different things in different
contexts:

- **`/economy take`** is an admin override — it should always succeed and just floor
  at zero, the same way EssentialsX-style admin commands behave. That's `subtract()`.
- **`/pay` and Vault's `withdrawPlayer`** are player-initiated transactions that must
  fail cleanly (no partial mutation, a clear "insufficient funds" response) rather
  than silently clamping — that's `withdraw()`/`transfer()`.

If you're adding a new caller, pick based on which of those two semantics it needs —
don't reuse `subtract()` for something that should be able to fail.

## Commands

See [COMMANDS.md](COMMANDS.md) for the full permission table. Summary:

- **`/economy give|take|set|reset <player> [amount]`** (`velto.economy.give/take/set/reset`,
  alias `eco`) — console-usable, follows the `KitResetCommand`/`SudoCommand` pattern
  (no `Player`-sender requirement, plain-text fallback messages for console). The target
  is resolved via `BaseCommand.resolveTarget`, so it may be **offline** (balances are
  UUID-keyed and loaded from disk on demand); the target-facing message is only sent when
  they happen to be online.
- **`/balance [player]`** (`velto.balance` / `velto.balance.others`, aliases `bal`/`money`) —
  `/balance <player>` is console-usable and resolves offline players too; bare `/balance`
  (self) stays player-only since the console has no balance.
- **`/pay <player> <amount>`** (`velto.pay`) — player-only, blocks self-pay, uses
  `transfer()` so insufficient funds leaves both balances untouched. Kept player-only by
  design — it debits the sender, which the console can't be.
- **`/baltop [page]`** (`velto.baltop`, aliases `balancetop`/`moneytop`) — see
  [`/baltop`](#baltop) below.

## `/baltop`

A paginated, highest-first balance leaderboard (10 entries per page), gated live by
`economy.yml: enabled` exactly like the other economy commands. Console-usable (it's a
read-only listing) with a plain-text fallback for each line, following the same
player-vs-console messaging split as `/balance`.

The interesting part is **how it reads balances without wrecking the userdata cache**.
`EconomyManager.getSortedBalances()` takes the union of currently-loaded players
(`UserdataManager.getCachedUuids()`) and every on-disk userdata file
(`UserdataManager.listStoredUuids()`), then for each UUID:

- prefers the **loaded** in-memory balance when present (`getLoadedDouble`, a cache-only
  read that returns `null` rather than lazily loading — so it reflects unsaved changes
  without re-creating a cache entry for anyone), and
- otherwise reads the balance **straight from disk** (`readDoubleFromDisk`), which never
  populates the cache.

This matters because [DATA_STORAGE.md](DATA_STORAGE.md#per-player-data-userdatamanager)
warns that a naive scan through `UserdataManager.getData(uuid)` for every player would
lazily load each offline player into the cache and leave them resident until restart —
exactly the "shop plugin scanning balances" footgun called out there. `/baltop` sidesteps
it entirely.

Every read `getSortedBalances()` performs is thread-safe, so `BalTopCommand` runs the
whole scan+sort on an async task and only hops back to the main thread to resolve player
names (`Bukkit.getOfflinePlayer(uuid)`) and send the messages. Requested pages past the
end clamp to the last page; a non-numeric page argument falls back to page 1.

## Vault integration

### Why it's safe to compile against Vault without requiring it

`VaultHook`/`VaultEconomyProvider` reference `net.milkbowl.vault.economy.Economy` (and
related types), added as a `compileOnly` Gradle dependency (via JitPack,
`com.github.MilkBowl:VaultAPI:1.7`) in all three modules. `compileOnly` means those
classes exist at *compile* time but are not bundled into the shaded jar — at
*runtime*, they only exist if the Vault plugin itself is installed on the server and
provides them.

The trick that makes this safe is the same one `PlaceholderManager` already uses for
PlaceholderAPI: the JVM resolves a symbolic class reference lazily, at the point a
piece of code actually executes that touches the class — not merely because some
*other* class references it in a method body. `VaultHook.refresh()` checks
`Bukkit.getPluginManager().isPluginEnabled("Vault")` **before** it ever does `new
VaultEconomyProvider()` or touches `Economy.class`/`ServicePriority`:

```java
public static void refresh() {
    boolean shouldRegister = EconomyManager.isEnabled()
            && EconomyManager.isVaultEnabled()
            && Bukkit.getPluginManager().isPluginEnabled("Vault");

    if (shouldRegister && !registered) {
        // only reached if Vault is actually present — safe to touch Vault classes here
        Bukkit.getServicesManager().register(Economy.class, new VaultEconomyProvider(), ...);
        ...
```

If Vault isn't installed, that branch never executes, so `VaultEconomyProvider` (which
`implements Economy`) never gets loaded, and there's no `NoClassDefFoundError`. This is
the pattern to copy for any future optional integration — see
[EXTENDING.md](EXTENDING.md#adding-an-optional-soft-dependency).

`Vault` is declared as a soft dependency (`softdepend` in `plugin.yml`,
`dependencies.server.Vault { load: BEFORE, required: false }` in `paper-plugin.yml`)
so that *if* it's present, it's already enabled by the time Velto's `onEnable` runs
`VaultHook.refresh()`.

### What happens when another plugin changes a balance through Vault

`VaultEconomyProvider` implements all 43 methods of Vault's `Economy` interface —
including the legacy `String`-name overloads (resolved via
`Bukkit.getOfflinePlayer(name)`) and the per-world overloads (Velto has no per-world
economy, so those just delegate to the global version) — and every balance-touching
method routes straight into `EconomyManager`:

```
other plugin: economy.depositPlayer(offlinePlayer, 50.0)
  → VaultEconomyProvider.depositPlayer(OfflinePlayer, double)
    → EconomyManager.add(uuid, amount)
      → setBalance(...) → UserdataManager.set(...) + UserdataManager.save(...)
```

That's identical to what `/economy give` does — there's no separate Vault-side ledger,
so nothing needs to be "synced". The in-memory balance updates immediately; the disk
write is queued async per the standard `UserdataManager` timing (see
[DATA_STORAGE.md](DATA_STORAGE.md)). If the target player is offline,
`UserdataManager.getData(uuid)` lazily loads their file from disk (creating one if
needed) — same caveat as noted there about that cache entry staying resident until
restart.

Bank accounts are explicitly unsupported (`hasBankSupport()` returns `false`, every
bank method returns `EconomyResponse.ResponseType.NOT_IMPLEMENTED`) — Velto has no
concept of shared/bank balances, only per-player.

### Feedback & fallback when Vault is missing

`VaultHook.refresh()` reports its outcome to the console so a misconfiguration isn't
silent:

- Vault present + `vault.enabled: true` → logs that it registered as the provider.
- `vault.enabled: true` but the **Vault plugin isn't installed** → logs a `WARNING`
  and falls back to disabled (the hook simply isn't registered — economy commands and
  balances keep working, only the Vault bridge is off). This is the case that used to
  be silent.
- `vault.enabled: false` → no warning; unregisters if it was previously active.

`VaultHook.isActive()` returns the real runtime state (true only when actually
registered), independent of the `vault.enabled` config *intent* — `/veltoreload`
prints it so you can confirm whether the bridge is live. `EconomyManager.load()` also
logs the module's enabled/disabled state and whether Vault was requested at startup.

### Reload behavior

`/veltoreload` calls `EconomyManager.load()` then `VaultHook.refresh()`. `refresh()`
is idempotent and bidirectional: flipping `vault.enabled` (or the whole module's
`enabled`) off and reloading unregisters the provider from Vault's `ServicesManager`;
flipping it back on and reloading re-registers it — no restart needed either way.

### Disabling

- **Whole module off:** `economy.yml: enabled: false` + `/veltoreload` (or restart) —
  hides all three commands and unregisters the Vault provider.
- **Just the Vault hook off** (keep Velto's own `/economy`/`/balance`/`/pay`, but let
  another economy plugin be Vault's provider instead): `economy.yml: vault.enabled:
  false` + `/veltoreload`.
