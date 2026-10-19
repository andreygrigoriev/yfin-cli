---
name: yfin
description: Fetch live Yahoo Finance market data with the `yfin` CLI. It covers quotes, price history, company profiles and key statistics, financial statements, valuation history, dividends and splits, earnings dates and estimates, analyst ratings and price targets, holders and insider trades, option chains, ETF and fund holdings, news, symbol search, predefined screens (gainers, losers, most active), sectors and industries, market overviews, and earnings, IPO and splits calendars. Use this skill whenever the user wants current or historical data about a stock, ETF, fund, index, currency or crypto. Examples include "how is AAPL doing", "compare MSFT and GOOGL margins", "plot NVDA over the last year", "when does Tesla report earnings", "what's the P/E of SAP.DE", "today's biggest gainers", "export the S&P 500 history to CSV", and "show me the option chain for SPY". Use it even if the user doesn't mention yfin or Yahoo. Prefer it over web search for any number it can fetch, because web pages are stale and hard to parse.
---

# yfin (Yahoo Finance CLI)

`yfin` is a native binary that fetches Yahoo Finance data. It writes data to **stdout** and
errors to **stderr**. It needs no API key.

If `command -v yfin` finds nothing, tell the user that it installs with
`brew install andreygrigoriev/tap/yfin` (macOS and Linux). Don't install it without asking.

## How to call it

- **Always pass `--format json` when you read the data yourself**, and filter it with `jq`. The
  default table is meant for people: it rounds and humanises numbers (`4.97T`) and truncates long
  text to the terminal width. Use `--format csv` when the user wants a file
  (`> prices.csv`). Show the default table only when the user just wants to look at the output in
  their terminal.
- JSON is an array of objects for lists and a single object for one record. Keys are snake_case
  and stay stable, so you can rely on them.
- **Percentages are fractions.** `change_pct: -0.0056` means -0.56%, and the same holds for
  margins, yields and growth rates. Multiply by 100 before you show a percentage. A few fields are
  exceptions and are already in percent, because that's how Yahoo sends them: `dividend_yield` in
  `info` (`0.31` means 0.31%), and `surprise_pct` in `earnings` and `calendar earnings` (`6.16`
  means a 6.16% beat). The fund `yield` field is a fraction like the others.
- Numbers are raw: `market_cap: 4984204193264` is about $4.98T. Dates are ISO strings. A missing
  value is `null`.
- Symbols are Yahoo symbols: `AAPL`, `BRK-B`, `^GSPC` (S&P 500), `^IXIC`, `SAP.DE`, `7203.T`,
  `BTC-USD`, `EURUSD=X`, `GC=F` (gold futures). If you're unsure of a symbol, run
  `yfin search "<name>"` first rather than guessing.
- Pass several symbols to one call (`yfin quote AAPL MSFT NVDA`) instead of running one call per
  symbol. A single call fetches them concurrently, adds a leading `symbol` column, and puts less
  load on Yahoo's rate limit.

## Which command

| Need | Command |
|---|---|
| Price now, day change, 52-week range | `quote SYM…` |
| Price bars (OHLC, adj close, volume) | `history SYM… [--period 1d\|5d\|1mo\|3mo\|6mo\|1y\|2y\|5y\|10y\|ytd\|max] [--interval 1m…1h\|1d\|5d\|1wk\|1mo\|3mo]`, or `--start D --end D` (end exclusive) |
| Profile, sector, P/E, margins, beta, targets | `info SYM` (one record with about 60 fields) |
| Income statement, balance sheet, cash flow | `financials income\|balance\|cashflow SYM [--quarterly\|--ttm]` |
| Valuation over time (market cap, P/E, EV/EBITDA…) | `valuation SYM [--quarterly\|--ttm]` |
| Next earnings date, dividend dates | `events SYM` |
| Past earnings: EPS estimate, actual, surprise | `earnings SYM [--limit N]` |
| Analyst estimates | `estimates earnings\|revenue\|history\|eps-trend\|eps-revisions\|growth SYM` |
| Analyst ratings and targets | `recommendations SYM [--summary]`, `upgrades SYM`, `price-target SYM` |
| Dividends, splits, capital gains | `dividends SYM`, `splits SYM`, `capital-gains SYM`, `actions SYM` (all, oldest first) |
| Shares outstanding over time | `shares SYM [--start D --end D]` |
| Holders and insider trades | `holders major\|institutional\|funds\|insiders\|insider-tx SYM` |
| ETF or mutual fund profile, holdings, sectors | `fund SYM [--holdings\|--sectors]` |
| Options | `options SYM` (expiration dates), then `options SYM --expiry YYYY-MM-DD [--calls\|--puts]` |
| News | `news SYM… [--count N] [--tab news\|all\|press-releases]` |
| Find a symbol by name | `search "QUERY" [--max N] [--news N]`, `lookup QUERY --type stock\|etf\|mutual-fund\|index\|future\|currency\|cryptocurrency` |
| Gainers, losers, most active, top ETFs… | `screen --list`, then `screen NAME [--size N] [--sort FIELD] [--asc]` |
| Sector and industry overviews and leaders | `sector KEY [--industries\|--top]` (e.g. `technology`), `industry KEY [--top\|--performing\|--growth]` |
| Market overview | `market us\|gb\|asia\|europe\|rates\|commodities\|currencies\|cryptocurrencies`, `market us --status` (trading hours) |
| Market-wide calendars | `calendar earnings\|ipo\|splits [--from D] [--to D] [--limit N]`. Add `earnings --all` for every company, not only today's most active |
| ESG scores, ISIN, SEC filings | `sustainability SYM`, `isin SYM`, `sec-filings SYM` |

Run `yfin <command> --help` when you need an option that isn't listed here.

Statements and valuation have one row per line item and one column per period. Line items are
keyed `line_item` in `financials` and `measure` in `valuation`. Period columns are named by date
(`2025_09_30`), and `valuation` also has a `current` column. Line item names are Yahoo's
CamelCase names (`TotalRevenue`, `GrossProfit`, `OperatingIncome`, `NetIncome`, `EBITDA`,
`DilutedEPS`). To pick a line item:

```bash
yfin financials income AAPL --format json | jq '.[] | select(.line_item == "TotalRevenue")'
yfin valuation AAPL --format json | jq '.[] | select(.measure == "PeRatio")'
```

More examples:

```bash
yfin quote AAPL MSFT --format json | jq -c '.[] | {symbol, last_price, change_pct}'
yfin history NVDA --period 1y --interval 1wk --format csv > nvda-weekly.csv
yfin info AAPL --format json | jq '{long_name, sector, trailing_pe, profit_margins, dividend_yield}'
yfin screen day_gainers --size 10 --format json
yfin calendar earnings --from 2026-10-19 --to 2026-10-23 --all --format json
```

## Errors and empty results

The exit code tells you what went wrong, and stderr has a one-line reason (`yfin: SYM: <reason>`):

| Code | Meaning | What to do |
|---|---|---|
| 0 | success, including an empty result (`[]`, `{}`) | Empty means Yahoo has no data for it. Say so and don't invent numbers. |
| 2 | bad usage | Fix the command. Check `--help`. |
| 3 | symbol not found | Find the right symbol with `yfin search`. |
| 4 | rate limited | Wait a little, then retry once. Batch symbols into one call. |
| 5, 6 | auth or network failure | Retry once. If it keeps failing, report it. `--verbose` shows the cause chain. |
| 7 | malformed data from Yahoo | Report it. Another command may hold the same data. |

With several symbols, the successful rows still print and each failed symbol gets its own line on
stderr. Use the rows you got and mention the failures.

## Reporting the numbers

- Mention the currency, which is the `currency` field in `quote` and `info` (for example, `SAP.DE`
  is quoted in EUR). Also say how fresh the data is: quotes may be delayed during market hours.
  For more context, `market_state` in `info` tells you whether the market is open.
- Do your own arithmetic (growth, ratios, averages) from the raw JSON numbers, not from the
  rounded table.
- This is market data, not advice. Don't present it as a recommendation to buy or sell.
