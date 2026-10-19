package io.github.andreygrigoriev.yfin.cli.cmd;

import static io.github.andreygrigoriev.yfin.cli.testing.CliRunner.TIMESERIES;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code valuation} reads all measures in one timeseries request. */
class ValuationCommandTest {
    private final CliRunner cli = new CliRunner();

    @Test
    void annual() {
        cli.fixture(TIMESERIES + "AAPL", "timeseries/valuation/aapl-annual.json");
        Golden.assertMatches(cli.run("valuation", "AAPL"), "valuation/normal.table");
        Golden.assertMatches(cli.run("valuation", "AAPL", "--format", "json"), "valuation/normal.json");
        Golden.assertMatches(cli.run("valuation", "AAPL", "--format", "csv"), "valuation/normal.csv");
    }

    @Test
    void quarterly() {
        cli.fixture(TIMESERIES + "AAPL", "timeseries/valuation/aapl-quarterly.json");
        Golden.assertMatches(cli.run("valuation", "AAPL", "--quarterly"), "valuation/quarterly.table");
        assertThat(cli.transport().requests().getLast().uri().getRawQuery()).contains("quarterlyMarketCap");
    }

    @Test
    void trailing() {
        cli.fixture(TIMESERIES + "AAPL", "timeseries/valuation/aapl-trailing.json");
        Golden.assertMatches(cli.run("valuation", "AAPL", "--ttm"), "valuation/ttm.table");
    }

    @Test
    void empty() {
        cli.fixture(TIMESERIES + "SPY", "timeseries/valuation/spy-quarterly.json");
        Golden.assertMatches(cli.run("valuation", "SPY", "--quarterly"), "valuation/empty.table");
        Golden.assertMatches(cli.run("valuation", "SPY", "--quarterly", "--format", "json"), "valuation/empty.json");
        Golden.assertMatches(cli.run("valuation", "SPY", "--quarterly", "--format", "csv"), "valuation/empty.csv");
    }

    @Test
    void error() {
        cli.onGet(TIMESERIES + "AAPL").respond(400, Fixtures.load("timeseries/error-bad-request.json"));
        Golden.assertMatches(cli.run("valuation", "AAPL"), "valuation/error.table");
        Golden.assertMatches(cli.run("valuation", "AAPL", "--format", "json"), "valuation/error.json");
    }

    @Test
    void quarterlyAndTtmTogetherIsAUsageError() {
        CliResult result = cli.run("valuation", "AAPL", "--quarterly", "--ttm");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("mutually exclusive");
        assertThat(cli.transport().requests()).isEmpty();
    }
}
