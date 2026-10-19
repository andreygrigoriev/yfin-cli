# yfin

[![CI](https://github.com/andreygrigoriev/yfin-cli/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/andreygrigoriev/yfin-cli/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/andreygrigoriev/yfin-cli)](https://github.com/andreygrigoriev/yfin-cli/releases/latest)
[![Homebrew](https://img.shields.io/badge/homebrew-andreygrigoriev%2Ftap-FBB040?logo=homebrew&logoColor=white)](https://github.com/andreygrigoriev/homebrew-tap)
[![Platforms](https://img.shields.io/badge/platforms-macOS%20%7C%20Linux-lightgrey)](#install)
[![GraalVM native](https://img.shields.io/badge/GraalVM-native%20image-E76F00)](#size-and-startup)
[![yfin-core](https://img.shields.io/maven-central/v/io.github.andreygrigoriev/yfin-core?label=yfin-core)](https://github.com/andreygrigoriev/yfin)
[![Claude Code skill](https://img.shields.io/badge/Claude%20Code-skill-D97757?logo=claude&logoColor=white)](#claude-code-skill)
[![License](https://img.shields.io/github/license/andreygrigoriev/yfin-cli)](LICENSE)

**Yahoo Finance market data in your terminal.** Quotes, price history, financial statements,
analyst estimates, options, holders, news, screens and calendars, as readable tables or as CSV and
JSON for your scripts.

```console
$ yfin history AAPL --period 5d
date          open    high     low   close  adj_close  volume
2026-10-13  344.67  348.15  340.74  341.27     341.27  38.98M
2026-10-14  344.61  347.09  341.72  342.96     342.96  50.06M
2026-10-15  346.95  347.85  341.60  342.00     342.00  39.77M
2026-10-16  346.09  347.82  340.46  343.46     343.46  59.90M
2026-10-19  340.98  346.00  340.11  342.35     342.35  18.11M
```

- **Fast, single binary.** A native executable for macOS and Linux that starts in ~15 ms. No Java,
  Python or Node needed.
- **Broad coverage.** 30 commands over stocks, ETFs, funds, indices, currencies and crypto.
- **Script-friendly.** `--format csv` and `--format json` give raw numbers with stable column
  names, and exit codes tell you exactly what went wrong.
- **Works with Claude.** A bundled [Claude Code skill](#claude-code-skill) lets Claude answer
  market questions from live data.

> [!WARNING]
> yfin is not affiliated with, endorsed by, or vetted by Yahoo, Inc. It uses Yahoo's publicly
> available endpoints, which are undocumented and can change without notice. Yahoo Finance data is
> for personal use only. You are responsible for following
> [Yahoo's terms of use](https://legal.yahoo.com/us/en/yahoo/terms/otos/index.html).

## Contents

- [Install](#install)
- [Quick start](#quick-start)
- [Commands](#commands)
- [Output formats](#output-formats)
- [Global options](#global-options)
- [Errors and exit codes](#errors-and-exit-codes)
- [Shell completion](#shell-completion)
- [Claude Code skill](#claude-code-skill)
- [Size and startup](#size-and-startup)
- [Comparison with other tools](#comparison-with-other-tools)
- [Building from source](#building-from-source)

## Install

With [Homebrew](https://brew.sh) on macOS or Linux:

```sh
brew install andreygrigoriev/tap/yfin
```

This installs the native binary (arm64 or x64) and its bash completion.

Without Homebrew, download `yfin-X.Y.Z-<os>-<arch>.tar.gz` from the
[latest release](https://github.com/andreygrigoriev/yfin-cli/releases/latest), check it against
`SHA256SUMS`, unpack it and put `yfin` on your `PATH`. Linux builds need glibc.

Check that it works:

```console
$ yfin --version
yfin 0.1.0
yfin-core 0.2.1
```

## Quick start

Symbols are the ones Yahoo uses: `AAPL`, `SAP.DE` (exchange suffix), `^GSPC` (index), `BTC-USD`
(crypto), `EURUSD=X` (currency). Not sure of a symbol? Search for it:

```sh
yfin search tesla
```

**Prices**

```sh
yfin quote AAPL MSFT BTC-USD                      # current price, change, day and year range
yfin history AAPL --period 1y --interval 1wk      # weekly bars for the last year
yfin history ^GSPC --start 2020-01-01 --end 2020-12-31
```

**Company and fundamentals**

```sh
yfin info AAPL                                    # profile, valuation, key statistics
yfin financials income AAPL --quarterly           # income statement, one column per quarter
yfin valuation MSFT                               # P/E, EV/EBITDA and more over time
yfin dividends KO
```

**Analysts and earnings**

```sh
yfin earnings NVDA --limit 4                      # past and upcoming EPS, with surprises
yfin price-target AAPL                            # low, mean, median and high targets
yfin recommendations AAPL --summary
```

```console
$ yfin price-target AAPL
current  342.35
low      215.00
mean     329.38
median   340.00
high     405.00
```

**Markets**

```sh
yfin screen day_gainers --size 10                 # today's top gainers (see: yfin screen --list)
yfin sector technology --top                      # the sector's largest companies
yfin calendar earnings --from 2026-10-19 --to 2026-10-23
yfin market us --status                           # is the market open?
```

**Scripting**

```sh
yfin screen day_gainers --format csv > gainers.csv
yfin info AAPL --format json | jq '.market_cap'
yfin quote AAPL MSFT --format json | jq -r '.[] | "\(.symbol) \(.last_price)"'
```

Every command explains its options with `--help`, for example `yfin history --help`.

## Commands

`SYM…` means the command accepts several symbols at once.

**Prices and history**

| Command | What it prints |
|---|---|
| `quote SYM…` | last price, change, day and year range, volume, market cap |
| `history SYM… [--period P \| --start D --end D] [--interval I] [--prepost] [--no-adjust]` | OHLC, adjusted close and volume bars |
| `options SYM [--expiry DATE] [--calls \| --puts]` | expiration dates, or the option chain of one expiration |

**Company profile**

| Command | What it prints |
|---|---|
| `info SYM` | profile, price, valuation and key statistics |
| `isin SYM` | the ISIN, empty when there is none |
| `events SYM` | next earnings date with estimates, dividend dates |
| `sustainability SYM` | ESG risk scores |
| `fund SYM [--holdings \| --sectors]` | profile, fees and asset split of an ETF or mutual fund |
| `sec-filings SYM` | SEC filings, newest first |

**Financials and corporate actions**

| Command | What it prints |
|---|---|
| `financials income\|balance\|cashflow SYM [--quarterly \| --ttm]` | financial statements, one column per period |
| `valuation SYM [--quarterly \| --ttm]` | market cap, P/E, EV/EBITDA and more, one column per period |
| `dividends SYM`, `splits SYM`, `capital-gains SYM`, `actions SYM` | corporate actions, oldest first |
| `shares SYM [--start D --end D]` | shares outstanding over time |

**Analysts and ownership**

| Command | What it prints |
|---|---|
| `earnings SYM [--limit N]` | earnings dates with EPS estimate, reported EPS and surprise |
| `estimates earnings\|revenue\|history\|eps-trend\|eps-revisions\|growth SYM` | analyst estimates |
| `recommendations SYM [--summary]` | analyst rating counts per month |
| `upgrades SYM` | analyst upgrades and downgrades, newest first |
| `price-target SYM` | low, mean, median and high price targets |
| `holders major\|institutional\|funds\|insiders\|insider-tx SYM` | ownership and insider transactions |

**News and search**

| Command | What it prints |
|---|---|
| `news SYM… [--count N] [--tab news\|all\|press-releases]` | latest articles |
| `search QUERY [--max N] [--news N]` | symbols matching a name, optionally with news |
| `lookup QUERY [--type T] [--count N]` | instruments of one type (stock, etf, cryptocurrency, …) |

**Markets, screens and calendars**

| Command | What it prints |
|---|---|
| `screen NAME [--size N] [--offset N] [--sort FIELD] [--asc]` | one of Yahoo's predefined screens; `screen --list` names them |
| `sector KEY [--industries \| --top]` | sector overview, its industries or its largest companies |
| `industry KEY [--top \| --performing \| --growth]` | industry overview or its leading companies |
| `market REGION [--status]` | headline instruments, or today's trading hours (`us`) |
| `calendar earnings\|ipo\|splits [--from D] [--to D] [--limit N]` | market-wide calendars |

`completion` prints the bash completion script (see [Shell completion](#shell-completion)).

Not supported yet: custom screener queries, research reports, history repair, live streaming, fish
completion and Windows.

## Output formats

Choose the format with `--format table|csv|json`. The default is `table`, and it stays `table` even
when you pipe the output, so ask for `csv` or `json` explicitly in scripts.

Column names are snake_case and identical in every format: the table header, the CSV header and
the JSON keys are the same words.

| | `table` | `csv` | `json` |
|---|---|---|---|
| missing value | `—` | empty | `null` |
| numbers | humanised (`1.23M`, `4.97T`) | raw | number |
| percentages | `1.15%` | fraction (`0.0115`) | fraction (`0.0115`) |
| dates | `2026-10-18` | ISO | ISO string |
| timestamps | `2026-10-18 09:30`, exchange time zone | ISO with offset | ISO string |
| list | aligned rows | header and rows | array of objects |
| single record | two columns, field and value | `field,value` rows | object |

A few things worth knowing:

- Commands over several symbols add a leading `symbol` column.
- Statements and valuation have one row per line item and one column per period (`2025_09_30`, …).
- Data goes to stdout; errors and notes go to stderr, so redirecting stdout to a file never
  captures error messages.
- On a terminal, tables are coloured (green and red changes) and long text is cut to fit the
  window. Set `NO_COLOR` or pass `--no-color` to turn colour off. When the output is not a
  terminal, there is no colour and nothing is cut.
- An empty result is not an error. The table is empty (with `no data` on stderr on a terminal),
  CSV prints only the header, JSON prints `[]` or `{}`, and the exit code is 0.

## Global options

These work with every command:

| Option | Default | Meaning |
|---|---|---|
| `--format table\|csv\|json` | `table` | output format |
| `--no-color` | | never colour table output |
| `--no-cache` | | don't read or write the on-disk cache |
| `--timeout DURATION` | `30s` | timeout per request, e.g. `30s`, `2m` |
| `--retries N` | `2` | retries on rate limits and server errors; `0` disables them |
| `--verbose` | | print the full cause of errors, and stack traces of unexpected ones |

yfin caches Yahoo's session cookie, crumb and exchange time zones in `$XDG_CACHE_HOME/yfin`
(or `~/.cache/yfin`), which saves a round trip on later runs. The cache is shared with the
[yfin](https://github.com/andreygrigoriev/yfin) library.

## Errors and exit codes

Errors print a single line on stderr, `yfin: <symbol>: <reason>`. When you ask for several
symbols and some fail, the successful rows still print and each failure gets its own line:

```console
$ yfin quote AAPL NOPE
symbol  last_price  change  change_pct  ...
AAPL        340.50    3.86       1.15%  ...
yfin: NOPE: Yahoo chart error Not Found: No data found, symbol may be delisted (NOPE)
$ echo $?
3
```

The exit code tells scripts what happened. With several failures, it is the code of the first one.

| Code | Meaning |
|---|---|
| 0 | success, including an empty result |
| 1 | unexpected error (`--verbose` shows the stack trace) |
| 2 | bad usage: unknown option or invalid value |
| 3 | symbol not found |
| 4 | rate limited by Yahoo; wait a bit and try again |
| 5 | authentication with Yahoo (cookie or crumb) failed |
| 6 | network or HTTP error |
| 7 | malformed or unusable data from Yahoo |
| 130 | interrupted (Ctrl-C) |

## Shell completion

Homebrew installs the bash completion for you. Otherwise, add one of these lines to your shell's
startup file:

```sh
# bash (~/.bashrc)
source <(yfin completion)

# zsh (~/.zshrc)
autoload -U bashcompinit && bashcompinit && source <(yfin completion)
```

## Claude Code skill

This repo ships a [Claude Code](https://claude.com/claude-code) skill
([`skills/yfin/SKILL.md`](skills/yfin/SKILL.md)). It teaches Claude when to use `yfin`, which
command answers which question, and how to read the output. With it, Claude can answer prompts like
"compare the margins of MSFT and GOOGL" or "when does NVDA report earnings?" from live data.

[Install `yfin`](#install) first, then add the skill as a plugin in Claude Code:

```
/plugin marketplace add andreygrigoriev/yfin-cli
/plugin install yfin@yfin-cli
```

Or copy the skill directory by hand:

```sh
git clone https://github.com/andreygrigoriev/yfin-cli
cp -r yfin-cli/skills/yfin ~/.claude/skills/
```

## Size and startup

Measured on an Apple Silicon Mac (GraalVM 25, `-Os`): the binary is 27.5 MiB, and
`yfin --version` starts in about 14 ms (median of 30 runs). For data commands, the round trip to
Yahoo takes far longer than yfin itself.

## Comparison with other tools

Other open-source command-line tools for Yahoo Finance data, as of October 2026:

- [yahoo-finance2](https://github.com/gadicc/yahoo-finance2) (Node): a library whose `yahoo-finance`
  CLI exposes its modules and prints Yahoo's raw JSON.
- [yahoo-finance-pp-cli](https://github.com/mvanhorn/printing-press-library/tree/main/library/commerce/yahoo-finance)
  (Go): a generated CLI with a local SQLite store, portfolios and watchlists.
- [yfinance-cli](https://github.com/narumiruna/yfinance-cli) (Python): a CLI over the
  [yfinance](https://github.com/ranaroussi/yfinance) library.
- [ticker-cli](https://github.com/sderosiaux/ticker-cli) (Go): prices and history only.

[ticker](https://github.com/achannarasappa/ticker) and [tickrs](https://github.com/tarkah/tickrs)
are live terminal dashboards rather than data CLIs, and yfinance and yahoo-finance2 are mainly
libraries.

**Features**

| | yfin | yahoo-finance2 CLI | yahoo-finance-pp-cli | yfinance-cli | ticker-cli |
|---|---|---|---|---|---|
| Version tested | 0.1.0 | 4.0.4 | 2026.7.1 | 0.0.1 | 0.1.0 |
| Runtime | none (native) | Node.js | none (Go) | Python 3.12+ | none (Go) |
| Quotes, several symbols | ✓ | ✓ | ✗¹ | – | ✓ |
| Price history | ✓ | ✓ | ✓ | ✓ | ✓ (up to 1y) |
| Profile and key statistics | ✓ | ✓ | – | ✓ | – |
| Financial statements | ✓ | ✓ | ✓ | ✓ | – |
| Options | ✓ | ✓ | ✓ | ✓ | – |
| News and symbol search | ✓ | ✓ | ✓ | ✓ | – |
| Predefined screens | ✓ | ✓ | ✓ | – | – |
| Analyst estimates and ratings | ✓ | ✓ | – | – | – |
| Holders and insider trades | ✓ | ✓ | partial | – | – |
| Market-wide calendars | ✓ | – | – | – | – |
| Sectors and industries | ✓ | – | – | ✓ | – |
| Output | table, CSV, JSON | raw Yahoo JSON | table, CSV, JSON | table, JSON | table, CSV, JSON |
| Agent integration | Claude skill | MCP, skill | MCP, skill | – | Claude skill |
| Extras | | | portfolio, watchlists, SQL | chart images | |

`–` means there is no command for it. A tool may still reach the data through a raw endpoint.

¹ HTTP 401 on our test network: its cookie/crumb handshake fails, and the documented workaround
is importing a Chrome session with `auth login --chrome`.

**Performance**

Measured on an Apple Silicon Mac over the same connection. Each figure is the median of 10 runs,
with the tools interleaved, after one warm-up run. pp-cli ran with
`--no-cache --data-source live`, so it fetched live data every time.

| | yfin | yahoo-finance2 CLI | yahoo-finance-pp-cli | yfinance-cli | ticker-cli |
|---|---|---|---|---|---|
| Size on disk | 28 MB | 39 MB (`node_modules`) | 12 MB | 255 MB (venv) | 6.2 MB |
| Startup (`--version` or `--help`) | 15 ms | 151 ms | 10 ms | 646 ms | 7 ms |
| 1 year of daily AAPL bars | 154 ms | 343 ms | 158 ms | 1,105 ms | 394 ms |
| Quotes for 4 symbols | 323 ms | 703 ms | failed¹ | – | 422 ms |
| Peak memory (history) | 36 MiB | 78 MiB | 22 MiB | 143 MiB | 18 MiB |

Network round trips dominate the data commands, so expect different numbers on another
connection. yfin and pp-cli are within noise of each other on history. The startup gap between the
native binaries and Node or Python carries over to every command.

## Building from source

yfin is a Java 25 project built on the [yfin](https://github.com/andreygrigoriev/yfin) library
(`io.github.andreygrigoriev:yfin-core`). Build and test with the Maven wrapper:

```sh
./mvnw verify
```

The native image needs GraalVM 25 as `JAVA_HOME`:

```sh
./mvnw -Pnative package   # builds target/yfin
./mvnw -Pnative test      # runs the test suite as a native image
```

`scripts/live-smoke.sh target/yfin` runs a few commands against Yahoo and checks the shape of the
JSON. See [`CLAUDE.md`](CLAUDE.md) for the code conventions and the release process.

## License

Apache License 2.0. See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE). Not affiliated with Yahoo, Inc.
