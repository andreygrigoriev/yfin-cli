package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code market} reads {@code /v6/finance/quote/marketSummary} and {@code /v6/finance/markettime};
 * the library fetches both together, whichever one is printed.
 */
class MarketCommandTest {
    private static final String SUMMARY = "/v6/finance/quote/marketSummary";
    private static final String TIME = "/v6/finance/markettime";

    private final CliRunner cli = new CliRunner();

    @Test
    void summary() {
        cli.onGet(SUMMARY).query("market", "US").respond(200, Fixtures.load("market/summary-us.json"));
        cli.onGet(TIME).query("market", "US").respond(200, Fixtures.load("market/markettime-us.json"));
        Golden.assertMatches(cli.run("market", "us"), "market/normal.table");
        Golden.assertMatches(cli.run("market", "us", "--format", "json"), "market/normal.json");
        Golden.assertMatches(cli.run("market", "us", "--format", "csv"), "market/normal.csv");
    }

    @Test
    void status() {
        cli.fixture(SUMMARY, "market/summary-us.json");
        cli.fixture(TIME, "market/markettime-us.json");
        Golden.assertMatches(cli.run("market", "us", "--status"), "market/status.table");
        Golden.assertMatches(cli.run("market", "us", "--status", "--format", "json"), "market/status.json");
        Golden.assertMatches(cli.run("market", "us", "--status", "--format", "csv"), "market/status.csv");
    }

    @Test
    void europe() {
        cli.onGet(SUMMARY).query("market", "EUROPE").respond(200, Fixtures.load("market/summary-europe.json"));
        cli.fixture(TIME, "market/markettime-us.json");
        Golden.assertMatches(cli.run("market", "europe"), "market/europe.table");
        // Yahoo reports trading hours for the U.S. only
        Golden.assertMatches(cli.run("market", "europe", "--status", "--format", "json"), "market/europe-status.json");
    }

    @Test
    void empty() {
        cli.fixture(SUMMARY, "market/summary-empty.json");
        cli.fixture(TIME, "market/markettime-us.json");
        Golden.assertMatches(cli.run("market", "us"), "market/empty.table");
        Golden.assertMatches(cli.run("market", "us", "--format", "json"), "market/empty.json");
        Golden.assertMatches(cli.run("market", "us", "--format", "csv"), "market/empty.csv");
    }

    @Test
    void badRequest() {
        cli.onGet(SUMMARY).respond(400, Fixtures.load("market/summary-bad-request.json"));
        cli.fixture(TIME, "market/markettime-us.json");
        Golden.assertMatches(cli.run("market", "gb"), "market/bad-request.table");
        Golden.assertMatches(cli.run("market", "gb", "--format", "json"), "market/bad-request.json");
    }

    @Test
    void missingMarketTime() {
        cli.fixture(SUMMARY, "market/summary-us.json");
        cli.fixture(TIME, "market/markettime-empty.json");
        Golden.assertMatches(cli.run("market", "us", "--status"), "market/no-market-time.table");
    }

    @Test
    void unknownRegion() {
        Golden.assertMatches(cli.run("market", "mars"), "market/unknown.table");
        assertThat(cli.transport().requests()).isEmpty();
    }
}
