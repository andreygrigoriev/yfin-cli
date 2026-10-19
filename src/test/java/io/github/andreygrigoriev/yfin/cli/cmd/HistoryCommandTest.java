package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.spi.HttpRequest;
import org.junit.jupiter.api.Test;

class HistoryCommandTest {
    private static final String CHART = "/v8/finance/chart/";

    private final CliRunner cli = new CliRunner();

    private void aaplMonth() {
        cli.onGet(CHART + "AAPL").query("range", "1mo").respond(200, Fixtures.load("chart/aapl-1d-1mo.json"));
    }

    private void notFound() {
        cli.onGet(CHART + "NOPE").respond(404, Fixtures.load("chart/not-found.json"));
    }

    @Test
    void normal() {
        aaplMonth();
        Golden.assertMatches(cli.run("history", "AAPL"), "history/normal.table");
        Golden.assertMatches(cli.run("history", "AAPL", "--format", "json"), "history/normal.json");
        Golden.assertMatches(cli.run("history", "AAPL", "--format", "csv"), "history/normal.csv");
    }

    @Test
    void emptyWeekend() {
        // a start/end window first resolves the exchange zone with a one-day chart
        cli.onGet(CHART + "AAPL").query("range", "1d").respond(200, Fixtures.load("chart/aapl-tz.json"));
        cli.onGet(CHART + "AAPL").hasQuery("period1").respond(200, Fixtures.load("chart/aapl-weekend-empty.json"));
        String[] args = {"history", "AAPL", "--start", "2026-10-10", "--end", "2026-10-11"};
        Golden.assertMatches(cli.run(args), "history/empty.table");
        Golden.assertMatches(cli.run(concat(args, "--format", "json")), "history/empty.json");
        Golden.assertMatches(cli.run(concat(args, "--format", "csv")), "history/empty.csv");
    }

    @Test
    void notFoundSymbol() {
        notFound();
        Golden.assertMatches(cli.run("history", "NOPE"), "history/not-found.table");
        Golden.assertMatches(cli.run("history", "NOPE", "--format", "json"), "history/not-found.json");
    }

    @Test
    void intradayWithPrePost() {
        cli.onGet(CHART + "AAPL")
                .query("interval", "15m")
                .query("includePrePost", "true")
                .respond(200, Fixtures.load("chart/aapl-15m-prepost.json"));
        Golden.assertMatches(
                cli.run("history", "AAPL", "--period", "1d", "--interval", "15m", "--prepost"),
                "history/intraday.table");
        Golden.assertMatches(
                cli.run("history", "AAPL", "--period", "1d", "--interval", "15m", "--prepost", "--format", "json"),
                "history/intraday.json");
    }

    @Test
    void multiSymbol() {
        aaplMonth();
        // no one-month BTC-USD recording: the five-day one stands in
        cli.onGet(CHART + "BTC-USD").query("range", "1mo").respond(200, Fixtures.load("chart/btc-usd-1d-5d.json"));
        Golden.assertMatches(cli.run("history", "AAPL", "BTC-USD"), "history/multi.table");
        Golden.assertMatches(cli.run("history", "AAPL", "BTC-USD", "--format", "json"), "history/multi.json");
    }

    @Test
    void multiSymbolPartialFailure() {
        aaplMonth();
        notFound();
        Golden.assertMatches(cli.run("history", "NOPE", "AAPL", "--format", "csv"), "history/partial.csv");
    }

    @Test
    void noAdjustKeepsRawPrices() {
        // the August 2020 window holds a dividend, so adjusted and raw closes differ
        cli.onGet(CHART + "AAPL").respond(200, Fixtures.load("chart/aapl-split-dividend-2020.json"));
        CliResult adjusted = cli.run("history", "AAPL", "--format", "csv");
        CliResult raw = cli.run("history", "AAPL", "--no-adjust", "--format", "csv");
        assertThat(raw.exitCode()).isZero();
        assertThat(raw.out()).isNotEqualTo(adjusted.out());
    }

    @Test
    void sendsThePeriodAndInterval() {
        cli.onGet(CHART + "AAPL").respond(200, Fixtures.load("chart/aapl-1d-1mo.json"));
        assertThat(cli.run("history", "AAPL", "--period", "3mo", "--interval", "1wk")
                        .exitCode())
                .isZero();
        HttpRequest request = cli.transport().requests().getLast();
        assertThat(request.uri().getRawQuery())
                .contains("range=3mo")
                .contains("interval=1wk")
                .contains("includePrePost=false");
    }

    @Test
    void periodWithStartIsAUsageError() {
        CliResult result = cli.run("history", "AAPL", "--period", "1mo", "--start", "2026-01-02");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.out()).isEmpty();
        assertThat(result.err()).contains("--period cannot be combined with --start or --end");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void periodWithEndIsAUsageError() {
        CliResult result = cli.run("history", "AAPL", "--period", "1mo", "--end", "2026-01-02");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--period cannot be combined with --start or --end");
    }

    @Test
    void startNotBeforeEndIsAUsageError() {
        CliResult result = cli.run("history", "AAPL", "--start", "2026-02-01", "--end", "2026-01-01");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("start 2026-02-01 is not before end 2026-01-01");
    }

    @Test
    void unknownIntervalIsAUsageError() {
        CliResult result = cli.run("history", "AAPL", "--interval", "7m");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--interval");
    }

    @Test
    void helpIsInheritedFromTheRoot() {
        CliResult result = cli.run("history", "--help");
        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).contains("Usage: yfin history", "--period=PERIOD", "--no-adjust", "--format=FORMAT");
    }

    private static String[] concat(String[] args, String... more) {
        String[] all = new String[args.length + more.length];
        System.arraycopy(args, 0, all, 0, args.length);
        System.arraycopy(more, 0, all, args.length, more.length);
        return all;
    }
}
