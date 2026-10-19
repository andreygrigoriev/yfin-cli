package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * {@code quote} reads, per symbol, the one-year daily chart, the five-day hourly chart and the
 * quote endpoint. {@code FastInfo} keeps only the daily bars of the year up to today, so the daily
 * fixtures are shifted to end six days ago ({@link Fixtures#chartEndingDaysAgo}): six days keeps the
 * year window's first day on a weekend for AAPL, leap year or not, so the goldens don't drift.
 */
class QuoteCommandTest {
    private static final String CHART = "/v8/finance/chart/";
    private static final String QUOTE = "/v7/finance/quote";
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private final CliRunner cli = new CliRunner();

    private void aapl() {
        cli.onGet(CHART + "AAPL")
                .query("range", "1y")
                .respond(200, Fixtures.chartEndingDaysAgo("chart/aapl-1d-1y.json", NEW_YORK, 6));
        cli.onGet(CHART + "AAPL").query("interval", "1h").respond(200, Fixtures.load("chart/aapl-1h-5d-prepost.json"));
        cli.onGet(QUOTE).query("symbols", "AAPL").respond(200, Fixtures.load("quote/aapl.json"));
    }

    private void btc() {
        cli.onGet(CHART + "BTC-USD")
                .query("range", "1y")
                .respond(200, Fixtures.chartEndingDaysAgo("chart/btc-usd-1d-1y.json", ZoneOffset.UTC, 6));
        // no hourly BTC-USD recording yet (see docs/fixtures.md): the five-day daily chart stands in;
        // FastInfo only takes the last close of each day from it
        cli.onGet(CHART + "BTC-USD").query("interval", "1h").respond(200, Fixtures.load("chart/btc-usd-1d-5d.json"));
        cli.onGet(QUOTE).query("symbols", "BTC-USD").respond(200, Fixtures.load("quote/btc-usd.json"));
    }

    private void notFound() {
        cli.onGet(CHART + "NOPE").respond(404, Fixtures.load("chart/not-found.json"));
        cli.onGet(QUOTE).query("symbols", "NOPE").respond(200, Fixtures.load("quote/not-found.json"));
    }

    @Test
    void single() {
        aapl();
        Golden.assertMatches(cli.run("quote", "AAPL"), "quote/normal.table");
        Golden.assertMatches(cli.run("quote", "AAPL", "--format", "json"), "quote/normal.json");
        Golden.assertMatches(cli.run("quote", "AAPL", "--format", "csv"), "quote/normal.csv");
    }

    @Test
    void multi() {
        aapl();
        btc();
        Golden.assertMatches(cli.run("quote", "AAPL", "BTC-USD"), "quote/multi.table");
        Golden.assertMatches(cli.run("quote", "AAPL", "BTC-USD", "--format", "json"), "quote/multi.json");
    }

    @Test
    void notFoundSymbol() {
        notFound();
        Golden.assertMatches(cli.run("quote", "NOPE"), "quote/not-found.table");
        Golden.assertMatches(cli.run("quote", "NOPE", "--format", "json"), "quote/not-found.json");
        Golden.assertMatches(cli.run("quote", "NOPE", "--format", "csv"), "quote/not-found.csv");
    }

    @Test
    void partialFailurePrintsTheRestAndExitsWithTheFailure() {
        aapl();
        notFound();
        Golden.assertMatches(cli.run("quote", "NOPE", "AAPL"), "quote/partial.table");
        Golden.assertMatches(cli.run("quote", "NOPE", "AAPL", "--format", "json"), "quote/partial.json");
    }

    @Test
    void symbolsAreNormalizedAndDeduplicated() {
        aapl();
        CliResult result = cli.run("quote", "aapl", "AAPL", "--format", "csv");
        assertThat(result.exitCode()).isZero();
        assertThat(result.out().lines()).hasSize(2);
        assertThat(result.out().lines().toList().get(1)).startsWith("AAPL,");
    }

    @Test
    void blankSymbolIsAUsageError() {
        CliResult result = cli.run("quote", " ");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Invalid symbol: symbol is blank");
    }

    @Test
    void symbolIsRequired() {
        CliResult result = cli.run("quote");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Missing required parameter: 'SYMBOL'");
    }
}
