# PlaceholderAPI Placeholders

Velto registers a PlaceholderAPI expansion under the identifier `velto`, so every
placeholder below is used as `%velto_<name>%`. Registration is handled by
[`PlaceholderManager`](MANAGERS.md) and only happens if PlaceholderAPI is installed
(`isPluginEnabled("PlaceholderAPI")` is checked before any PAPI class is touched — see
[EXTENDING.md](EXTENDING.md#adding-an-optional-soft-dependency)). Without PlaceholderAPI
the placeholders simply don't resolve; the plugin still runs.

All placeholders are resolved against the requesting **player** — a `null` player yields
an empty string. An unknown identifier returns `null` (PlaceholderAPI leaves the raw
`%velto_...%` text in place), and any resolver that throws is caught and returns `""`.

## Catalog

| Placeholder | Value | Notes |
|---|---|---|
| `%velto_afk%` | `true` / `false` | Whether the player is currently AFK ([`AfkManager`](MANAGERS.md)). |
| `%velto_homes_count%` | integer | Number of homes the player has set. |
| `%velto_homes_list%` | comma-separated names | The player's home names, e.g. `home, base, mine`. Empty string if none. |
| `%velto_balance%` | raw number | Balance formatted to `economy.yml`'s `currency.decimal-places`, no symbol or separators (e.g. `1234.50`). |
| `%velto_balance_formatted%` | formatted amount | Balance via `EconomyManager.format()` — currency symbol + thousands separators (e.g. `$1,234.50`). |
| `%velto_currency_symbol%` | string | The configured currency symbol (`currency.symbol`). |
| `%velto_currency_name%` | string | Plural currency name (`currency.name-plural`). |
| `%velto_currency_name_singular%` | string | Singular currency name (`currency.name-singular`). |

### Economy module toggle

Every economy placeholder (`balance`, `balance_formatted`, `currency_*`) resolves to an
**empty string** while the economy module is disabled (`economy.yml: enabled: false`).
Because the check happens inside the resolver at request time, this responds live to
`/veltoreload` — no restart needed. See [ECONOMY.md](ECONOMY.md).

## Adding more placeholders

New placeholders are registered in `PlaceholderManager.registerDefaultPlaceholders()`
via `registerPlaceholder("name", player -> ...)`, following the existing entries. The
registry is public (`registerPlaceholder` / `unregisterPlaceholder` / `hasPlaceholder`),
so other managers can contribute placeholders without depending on PlaceholderAPI
directly. Keep the resolver cheap — it can be called on every chat line, scoreboard
tick, or tab-list refresh — and null-safe. Update the catalog table above when you add
one.
