package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code earnings} POSTs a query to the visualization endpoint, then looks up the exchange zone
 * with a one-day chart when there are rows. SPY and an unknown symbol both give no rows; Yahoo's
 * error envelope ({@code bad-entity}) is a data error.
 */
class EarningsCommandTest {
    private static final String VISUALIZATION = "/v1/finance/visualization";
    private static final String CHART = "/v8/finance/chart/";

    private final CliRunner cli = new CliRunner();

    @Test
    void normal() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/aapl.json"));
        cli.fixture(CHART + "AAPL", "chart/aapl-tz.json");
        Golden.assertMatches(cli.run("earnings", "AAPL"), "earnings/normal.table");
        Golden.assertMatches(cli.run("earnings", "AAPL", "--format", "json"), "earnings/normal.json");
        Golden.assertMatches(cli.run("earnings", "AAPL", "--format", "csv"), "earnings/normal.csv");
    }

    @Test
    void limitIsSentAsSize() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/sap-de.json"));
        cli.onGet(CHART + "SAP.DE")
                .respond(200, Fixtures.load("chart/aapl-tz.json").replace("America/New_York", "Europe/Berlin"));
        CliResult result = cli.run("earnings", "SAP.DE", "--limit", "8");
        assertThat(result.exitCode()).isZero();
        assertThat(cli.transport().requests().stream()
                        .filter(r -> r.uri().getPath().equals(VISUALIZATION))
                        .findFirst()
                        .orElseThrow()
                        .bodyAsString()
                        .orElseThrow())
                .contains("\"size\":8");
    }

    @Test
    void missingEstimates() {
        // CBA.AX has dates only; its zone comes from the AAPL zone recording with the zone swapped
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/cba-ax.json"));
        cli.onGet(CHART + "CBA.AX")
                .respond(200, Fixtures.load("chart/aapl-tz.json").replace("America/New_York", "Australia/Sydney"));
        Golden.assertMatches(cli.run("earnings", "CBA.AX"), "earnings/cba-ax.table");
    }

    @Test
    void empty() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/spy.json"));
        Golden.assertMatches(cli.run("earnings", "SPY"), "earnings/empty.table");
        Golden.assertMatches(cli.run("earnings", "SPY", "--format", "json"), "earnings/empty.json");
        Golden.assertMatches(cli.run("earnings", "SPY", "--format", "csv"), "earnings/empty.csv");
    }

    @Test
    void unknownSymbolIsEmpty() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/unknown.json"));
        Golden.assertMatches(cli.run("earnings", "ZZZZNOTREAL", "--format", "json"), "earnings/empty.json");
    }

    @Test
    void errorEnvelope() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("earnings/bad-entity.json"));
        Golden.assertMatches(cli.run("earnings", "AAPL"), "earnings/error.table");
        Golden.assertMatches(cli.run("earnings", "AAPL", "--format", "json"), "earnings/error.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101"})
    void limitOutOfRangeIsUsageError(String limit) {
        CliResult result = cli.run("earnings", "AAPL", "--limit", limit);
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--limit must be in 1..100, was " + limit);
        assertThat(cli.transport().requests()).isEmpty();
    }
}
