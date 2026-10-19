# yfin-cli

`yfin` is a command-line tool for Yahoo Finance market data. It is built on the
[yfin](https://github.com/andreygrigoriev/yfin) Java library (`io.github.andreygrigoriev:yfin-core`)
and compiled to a GraalVM native image, so it starts instantly and needs no JVM.

```console
$ yfin quote AAPL
symbol  last_price  change  change_pct  previous_close    open  day_high  day_low  volume  market_cap  year_high  year_low  currency  exchange
AAPL        340.50    3.86       1.15%          336.64  338.16    342.63   337.60   9.00M       4.97T     345.34    243.42  USD       NMS
```

> [!WARNING]
> yfin is not affiliated with, endorsed by, or vetted by Yahoo, Inc. It uses Yahoo's publicly
> available endpoints, which are undocumented and can change without notice. Yahoo Finance data is
> for personal use only. You are responsible for following
> [Yahoo's terms of use](https://legal.yahoo.com/us/en/yahoo/terms/otos/index.html).

## Install

```sh
brew install andreygrigoriev/tap/yfin
```

The formula ships native binaries for macOS and Linux (glibc), arm64 and x64, and installs the bash
completion. Without Homebrew, download `yfin-X.Y.Z-<os>-<arch>.tar.gz` from the GitHub
release, check it against `SHA256SUMS`, and put `yfin` on your `PATH`.

`yfin --version` prints the CLI version and the `yfin-core` version it was built with.

## Commands

Every command takes `--help`. Symbols are Yahoo symbols (`AAPL`, `BTC-USD`, `^GSPC`, `SAP.DE`).

| Command | What it prints |
|---|---|
| `quote SYM…` | last price, change, day and year range, volume, market cap |
| `history SYM… [--period P \| --start D --end D] [--interval I] [--prepost] [--no-adjust]` | OHLC, adjusted close and volume bars |
| `info SYM` | profile, price, valuation and key statistics |
| `isin SYM` | the ISIN, empty when there is none |
| `events SYM` | next earnings date with estimates, dividend dates |
| `sustainability SYM` | ESG risk scores |
| `fund SYM [--holdings \| --sectors]` | profile, fees and asset split of an ETF or mutual fund |
| `dividends`, `splits`, `capital-gains`, `actions SYM` | corporate actions, oldest first |
| `shares SYM [--start D --end D]` | shares outstanding over time |
| `sec-filings SYM` | SEC filings, newest first |
| `financials income\|balance\|cashflow SYM [--quarterly \| --ttm]` | financial statements, one column per period |
| `valuation SYM [--quarterly \| --ttm]` | market cap, P/E, EV/EBITDA and more, one column per period |
| `holders major\|institutional\|funds\|insiders\|insider-tx SYM` | ownership and insider transactions |
| `estimates earnings\|revenue\|history\|eps-trend\|eps-revisions\|growth SYM` | analyst estimates |
| `recommendations SYM [--summary]`, `upgrades SYM`, `price-target SYM` | analyst ratings and targets |
| `earnings SYM [--limit N]` | earnings dates with EPS estimate, reported EPS and surprise |
| `options SYM [--expiry DATE] [--calls \| --puts]` | expiration dates, or the chain of one expiration |
| `news SYM… [--count N] [--tab news\|all\|press-releases]` | latest articles |
| `search QUERY [--max N] [--news N]` | symbols matching a name, optionally with news |
| `lookup QUERY [--type T] [--count N]` | instruments of one type (stock, etf, cryptocurrency, …) |
| `screen NAME [--size N] [--offset N] [--sort FIELD] [--asc]`, `screen --list` | Yahoo's predefined screens |
| `sector KEY [--industries \| --top]` | sector overview, its industries or its largest companies |
| `industry KEY [--top \| --performing \| --growth]` | industry overview or its leading companies |
| `market REGION [--status]` | headline instruments, or today's trading hours (`us`) |
| `calendar earnings\|ipo\|splits [--from D] [--to D] [--limit N]` | market-wide calendars |
| `completion` | bash completion script |

Out of scope for now: custom screener queries, research reports, history repair, live streaming,
fish completion and Windows.

Examples:

```sh
yfin history AAPL MSFT --period 6mo --interval 1wk
yfin financials income AAPL --quarterly
yfin options AAPL --expiry 2026-11-20 --calls
yfin calendar earnings --from 2026-10-19 --to 2026-10-23 --all
yfin screen day_gainers --size 10 --format csv > gainers.csv
yfin info AAPL --format json | jq '.market_cap'
```

## Claude Code skill

The repo ships a [Claude Code](https://claude.com/claude-code) skill, `skills/yfin/SKILL.md`. It
teaches Claude when to reach for `yfin`, which command answers which question, and how to read the
output (JSON, fractions for percentages, exit codes). With it, prompts like "compare the margins of
MSFT and GOOGL" or "when does NVDA report earnings?" get answered from live data. Install `yfin`
first, then add the skill as a plugin:

```
/plugin marketplace add andreygrigoriev/yfin-cli
/plugin install yfin@yfin-cli
```

Or copy the skill directory by hand:

```sh
git clone https://github.com/andreygrigoriev/yfin-cli
cp -r yfin-cli/skills/yfin ~/.claude/skills/
```

## Global options

Every command accepts these:

| Option | Default | Meaning |
|---|---|---|
| `--format table\|csv\|json` | `table` | output format |
| `--no-color` | | never colour table output |
| `--no-cache` | | don't read or write the on-disk cache |
| `--timeout DURATION` | `30s` | timeout per request, e.g. `30s`, `2m` |
| `--retries N` | `2` | retries on rate limits and server errors, `0` disables |
| `--verbose` | | print the cause chain of errors, and stack traces of unexpected ones |

The cache (Yahoo cookie, crumb and exchange time zones) lives in `$XDG_CACHE_HOME/yfin`, or
`~/.cache/yfin`, and is shared with the library.

## Output formats

Data goes to stdout, errors and notes to stderr. The format is always the one you ask for: it never
changes because stdout is a pipe. Column names are snake_case and are the same in every format.

| | `table` | `csv` | `json` |
|---|---|---|---|
| missing value | `—` | empty | `null` |
| numbers | humanised (`1.23M`, `4.97T`) | raw | number |
| percentages | `1.15%` | raw fraction | number (fraction) |
| dates | `2026-10-18` | ISO | ISO string |
| timestamps | `2026-10-18 09:30`, exchange time zone | ISO with offset | ISO string |
| list | aligned rows | header and rows | array of objects |
| single record | two columns, field and value | `field,value` rows | object |

Statements and valuation have one column per period (`2025_09_30`, …) and one row per line item.
Commands over several symbols add a leading `symbol` column.

On a terminal, tables are coloured (green/red changes) and long text is cut to the terminal width.
`NO_COLOR` or `--no-color` turns colour off. Off a terminal there is no colour and no truncation.

An empty result is not an error: the table is empty (`no data` on stderr on a terminal), CSV prints
only the header, JSON prints `[]` or `{}`, and the exit code is 0.

## Exit codes

| Code | Cause |
|---|---|
| 0 | success, including an empty result |
| 1 | unexpected error (stack trace with `--verbose`) |
| 2 | bad usage: unknown option, invalid value |
| 3 | symbol not found |
| 4 | rate limited by Yahoo |
| 5 | authentication (cookie/crumb) failed |
| 6 | network or HTTP error |
| 7 | malformed or unusable data from Yahoo |
| 130 | interrupted |

Errors print one line to stderr: `yfin: <symbol>: <reason>`. With several symbols, the successful
rows still print, each failed symbol gets its own line, and the exit code is that of the first
failure:

```console
$ yfin quote AAPL NOPE
symbol  last_price  change  change_pct  ...
AAPL        340.50    3.86       1.15%  ...
yfin: NOPE: Yahoo chart error Not Found: No data found, symbol may be delisted (NOPE)
$ echo $?
3
```

## Shell completion

Homebrew installs the bash completion for you. For zsh, or without Homebrew:

```sh
# bash
source <(yfin completion)
# zsh
autoload -U bashcompinit && bashcompinit && source <(yfin completion)
```

## Size and startup

Measured on an Apple Silicon Mac (GraalVM 25, `-Os`): the binary is 27.5 MiB, and
`yfin --version` starts in about 14 ms (median of 30 runs). Each data command is dominated by the
round trip to Yahoo.

## Build

Java 25 and the Maven wrapper:

```sh
./mvnw verify
```

Native image, with GraalVM 25 as `JAVA_HOME`:

```sh
./mvnw -Pnative package   # builds target/yfin
./mvnw -Pnative test      # runs the test suite as a native image
```

`scripts/live-smoke.sh target/yfin` runs a few commands against Yahoo and checks the JSON shape.
See `CLAUDE.md` for the conventions and the release process.

## License

Apache License 2.0. See `LICENSE` and `NOTICE`. Not affiliated with Yahoo, Inc.
