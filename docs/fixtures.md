# Fixtures for command golden tests

Golden tests replay Yahoo responses recorded by the library (`yfin-core` test-jar,
`fixtures/<endpoint>/<case>.json`, loaded with `Fixtures.load`). This file maps each planned
command to those fixtures and lists the gaps. A gap is closed in the yfin repo by adding a case to
`FixtureRecorder.CASES`, recording it and releasing a new `yfin-core` test-jar; until then the
golden test uses the closest existing fixture or a documented `*-synthetic` body.

Every request first hits the cookie and crumb bootstrap, which `CliRunner` registers.

## Command → fixtures

| Command | Endpoint(s) | Normal | Empty | Error |
|---|---|---|---|---|
| `quote SYM…` | `/v8/finance/chart`, `/v7/finance/quote` | `chart/aapl-1d-1y`, `quote/aapl` (+ `btc-usd`, `spy` for multi) | — | `quote/not-found`, `chart/not-found` |
| `history SYM…` | `/v8/finance/chart` | `chart/aapl-1d-1mo`, `aapl-15m-prepost`, `aapl-1h-5d-prepost`, `btc-usd-1d-5d` | `chart/aapl-weekend-empty` | `chart/not-found` |
| `info SYM` | `/v10/finance/quoteSummary`, `/v7/finance/quote` | `quoteSummary/aapl` + `quote/aapl` | `quoteSummary/btc-usd` (sparse) | `quoteSummary/not-found` |
| `dividends`, `splits`, `actions SYM` | `/v8/finance/chart` | `chart/aapl-split-dividend-2020` | `chart/aapl-1d-1mo` (no events) | `chart/not-found` |
| `capital-gains SYM` | `/v8/finance/chart` | `chart/schd-1wk-capital-gains` | `chart/aapl-1d-1mo` | `chart/not-found` |
| `shares SYM` | `/ws/fundamentals-timeseries/…/timeseries`, chart (timezone) | `shares/aapl` | `shares/btc-usd` | `shares/unknown` |
| `isin SYM` | Business Insider ISIN search, `/v7/finance/quote` | `isin/aapl.txt`, `isin/sap-de.txt` | `isin/unknown.txt` | ⚠️ gap |
| `sec-filings SYM` | quoteSummary `secFilings` | `quoteSummary/secfilings/aapl` | `quoteSummary/secfilings/spy` | `quoteSummary/not-found` |
| `sustainability SYM` | quoteSummary `esgScores` | `quoteSummary/analysis/msft-esg-synthetic` | `quoteSummary/analysis/aapl-esg` | `quoteSummary/not-found` |
| `fund SYM` | quoteSummary `fundProfile`, `topHoldings` | `quoteSummary/funds/spy`, `funds/agthx` | `quoteSummary/funds/aapl` | `quoteSummary/not-found` |
| `events SYM` | quoteSummary `calendarEvents` | `quoteSummary/analysis/aapl` | `quoteSummary/analysis/spy` | `quoteSummary/not-found` |
| `financials income SYM` | timeseries | `timeseries/aapl-annual-financials-*`, `aapl-quarterly-financials-*`, `aapl-trailing-financials-*`, `jpm-*`, `spy-*` | `timeseries/spy-annual-financials-*` | `timeseries/error-bad-request` |
| `financials balance SYM` | timeseries | `timeseries/aapl-annual-balance-sheet-*`, `jpm-annual-balance-sheet-*` | ⚠️ gap | `timeseries/error-bad-request` |
| `financials cashflow SYM` | timeseries | `timeseries/aapl-trailing-cash-flow-*` | ⚠️ gap | `timeseries/error-bad-request` |
| `valuation SYM` | timeseries (valuation) | `timeseries/valuation/aapl-annual`, `aapl-quarterly`, `aapl-trailing` | `timeseries/valuation/spy-quarterly` | `timeseries/error-bad-request` |
| `holders major\|institutional\|funds\|insiders\|insider-tx SYM` | quoteSummary holder modules | `quoteSummary/holders/aapl` (`sap-de` has no insider transactions) | `quoteSummary/holders/spy`, `btc-usd` | `quoteSummary/not-found` |
| `estimates earnings\|revenue\|history\|eps-trend\|eps-revisions SYM` | quoteSummary `earningsTrend`, `earningsHistory` | `quoteSummary/analysis/aapl`, `sap-de` | `quoteSummary/analysis/spy`, `btc-usd` | `quoteSummary/not-found` |
| `estimates growth SYM` | quoteSummary trends + timeseries PEG | `quoteSummary/analysis/aapl` + `timeseries/peg/aapl` | `timeseries/peg/spy` | `quoteSummary/not-found` |
| `recommendations SYM [--summary]` | quoteSummary `recommendationTrend` | `quoteSummary/analysis/aapl` | `quoteSummary/analysis/spy` | `quoteSummary/not-found` |
| `upgrades SYM` | quoteSummary `upgradeDowngradeHistory` | `quoteSummary/analysis/aapl` | `quoteSummary/analysis/sap-de` (module absent) | `quoteSummary/not-found` |
| `price-target SYM` | quoteSummary `financialData` | `quoteSummary/analysis/aapl` | `quoteSummary/analysis/spy` | `quoteSummary/not-found` |
| `earnings SYM [--limit]` | `/v1/finance/visualization`, chart (timezone) | `earnings/aapl`, `sap-de`, `cba-ax` | `earnings/spy` | `earnings/unknown`, `earnings/bad-entity` |
| `options SYM [--expiry]` | `/v7/finance/options` | `options/aapl`, `options/aapl-2029-01-19` | `options/aapl-not-an-expiration` | `options/unknown`, `options/bad-date` |
| `news SYM…` | `/xhr/ncp` (news tabs) | `news/aapl-ncp-synthetic` | `news/empty-ncp-synthetic` | `news/not-found` |
| `search QUERY [--news]` | `/v1/finance/search` | `search/apple`, `search/tech`, `news/search-aapl` | `search/empty` | `search/bad-request` |
| `lookup QUERY [--type]` | `/v1/finance/lookup` | `lookup/aapl-all`, `btc-crypto`, `gold-etf` | `lookup/empty` | `lookup/bad-type` |
| `screen NAME` | `/v1/finance/screener/predefined/saved` | `screener/predefined-day-gainers`, `predefined-most-actives`, `predefined-offset` | ⚠️ gap | `screener/predefined-unknown` |
| `sector KEY` | `/v1/finance/sectors/<key>` | `sector/technology` | `sector/minimal` | `sector/error`, `sector/unknown.html` |
| `industry KEY` | `/v1/finance/industries/<key>` | `industry/semiconductors` | `industry/minimal` | `industry/unknown.html` |
| `market REGION [--status]` | `/v6/finance/quote/marketSummary`, `/v6/finance/markettime` | `market/summary-us`, `summary-europe`, `markettime-us` | `market/summary-empty`, `markettime-empty` | `market/summary-bad-request` |
| `calendar earnings` | visualization + screener (`most_actives`) | `calendar/earnings-week`, `earnings-week-mcap` + `calendar/most-actives` | `calendar/earnings-empty` | `calendar/error` |
| `calendar ipo` | visualization | `calendar/ipo-week` | `calendar/ipo-empty` | `calendar/error` |
| `calendar splits` | visualization | `calendar/splits-week` | `calendar/splits-empty` | `calendar/error` |
| `completion` | none | — | — | — |

The endpoint paths are what the CLI tests register on `CliRunner`; check the library endpoint
class for the exact path and query parameters when writing a test. `quote` uses `fastInfo()`,
which reads the quote endpoint and the 1y daily / 5d hourly charts.

## Gaps (new `FixtureRecorder.CASES` in yfin)

- `isin/error`: the ISIN lookup service failing (non-200 or garbage body). Until then the error
  case uses a synthetic suggest-service failure (`respond(403, "forbidden")`), named in the test.
- `timeseries/aapl-annual-balance-sheet` / `cash-flow` for a symbol with no statements (e.g. an
  ETF such as SPY), for the `financials balance|cashflow` empty cases. `spy-annual-financials-*`
  covers only the income statement.
- `screener/predefined-empty`: a predefined screener with no quotes. Until then the empty case
  uses an offset past the end, recorded as a new case.
- `quote` multi-symbol needs the 1y daily and 5d hourly charts per extra symbol (`btc-usd`,
  `spy`); only AAPL has the full set (`chart/aapl-1d-1y`, `aapl-1h-5d-prepost`).
