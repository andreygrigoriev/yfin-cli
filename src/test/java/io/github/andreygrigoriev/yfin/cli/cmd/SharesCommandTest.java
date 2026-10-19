package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.spi.HttpRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * {@code shares} reads the timeseries endpoint; the library first looks up the exchange zone with a
 * one-day chart. BTC-USD has no share counts, and an unknown symbol fails that zone lookup.
 */
class SharesCommandTest {
    private static final String CHART = "/v8/finance/chart/";
    private static final String TIMESERIES = "/ws/fundamentals-timeseries/v1/finance/timeseries/";
    private static final String[] AAPL = {"shares", "AAPL", "--start", "2025-01-01", "--end", "2026-10-13"};

    private final CliRunner cli = new CliRunner();

    private void aapl() {
        cli.fixture(CHART + "AAPL", "chart/aapl-tz.json");
        cli.fixture(TIMESERIES + "AAPL", "shares/aapl.json");
    }

    @Test
    void normal() {
        aapl();
        Golden.assertMatches(cli.run(AAPL), "shares/normal.table");
        Golden.assertMatches(cli.run(concat(AAPL, "--format", "json")), "shares/normal.json");
        Golden.assertMatches(cli.run(concat(AAPL, "--format", "csv")), "shares/normal.csv");
    }

    @Test
    void empty() {
        cli.fixture(CHART + "BTC-USD", "chart/btc-usd-1d-5d.json");
        cli.fixture(TIMESERIES + "BTC-USD", "shares/btc-usd.json");
        Golden.assertMatches(cli.run("shares", "BTC-USD"), "shares/empty.table");
        Golden.assertMatches(cli.run("shares", "BTC-USD", "--format", "json"), "shares/empty.json");
        Golden.assertMatches(cli.run("shares", "BTC-USD", "--format", "csv"), "shares/empty.csv");
    }

    @Test
    void notFound() {
        cli.onGet(CHART + "NOPE").respond(404, Fixtures.load("chart/not-found.json"));
        cli.fixture(TIMESERIES + "NOPE", "shares/unknown.json");
        Golden.assertMatches(cli.run("shares", "NOPE"), "shares/not-found.table");
        Golden.assertMatches(cli.run("shares", "NOPE", "--format", "json"), "shares/not-found.json");
    }

    @Test
    void badRequest() {
        // synthetic: the timeseries endpoint rejecting the request, as in the library's SharesEndpointTest
        cli.fixture(CHART + "AAPL", "chart/aapl-tz.json");
        cli.onGet(TIMESERIES + "AAPL")
                .respond(
                        400,
                        "{\"finance\":{\"result\":null,\"error\":{\"code\":\"Bad Request\",\"description\":\"x\"}}}");
        Golden.assertMatches(cli.run(AAPL), "shares/bad-request.table");
    }

    @Test
    void sendsTheWindowInTheExchangeZone() {
        aapl();
        assertThat(cli.run(AAPL).exitCode()).isZero();
        HttpRequest request = cli.transport().requests().getLast();
        ZoneId ny = ZoneId.of("America/New_York");
        assertThat(request.uri().getRawQuery())
                .contains("symbol=AAPL")
                .contains("period1="
                        + ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ny).toEpochSecond())
                .contains("period2="
                        + ZonedDateTime.of(2026, 10, 13, 0, 0, 0, 0, ny).toEpochSecond());
    }

    @Test
    void startNotBeforeEndIsAUsageError() {
        CliResult result = cli.run("shares", "AAPL", "--start", "2026-02-01", "--end", "2026-02-01");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--start 2026-02-01 is not before --end 2026-02-01");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void badDateIsAUsageError() {
        CliResult result = cli.run("shares", "AAPL", "--start", "yesterday");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--start");
    }

    private static String[] concat(String[] args, String... more) {
        String[] all = new String[args.length + more.length];
        System.arraycopy(args, 0, all, 0, args.length);
        System.arraycopy(more, 0, all, args.length, more.length);
        return all;
    }
}
