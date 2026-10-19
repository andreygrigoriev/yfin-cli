package io.github.andreygrigoriev.yfin.cli.cmd;

import static io.github.andreygrigoriev.yfin.cli.testing.CliRunner.TIMESERIES;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code financials income|balance|cashflow} read the chunked timeseries endpoint; {@link
 * CliRunner#timeseries} serves each chunk its recorded response. There are no recordings of an
 * empty balance sheet or cash flow statement, so those empty cases answer every chunk with a
 * synthetic result list without series, as Yahoo does for a symbol without fundamentals.
 */
class FinancialsCommandTest {
    private static final String NO_SERIES = "{\"timeseries\":{\"result\":[],\"error\":null}}";

    private final CliRunner cli = new CliRunner();

    private void badRequest(String symbol) {
        cli.onGet(TIMESERIES + symbol).respond(400, Fixtures.load("timeseries/error-bad-request.json"));
    }

    @Test
    void incomeAnnual() {
        cli.timeseries("AAPL", "timeseries/aapl-annual-financials");
        Golden.assertMatches(cli.run("financials", "income", "AAPL"), "financials/income/normal.table");
        Golden.assertMatches(
                cli.run("financials", "income", "AAPL", "--format", "json"), "financials/income/normal.json");
        Golden.assertMatches(
                cli.run("financials", "income", "AAPL", "--format", "csv"), "financials/income/normal.csv");
    }

    @Test
    void incomeQuarterly() {
        cli.timeseries("AAPL", "timeseries/aapl-quarterly-financials");
        Golden.assertMatches(
                cli.run("financials", "income", "AAPL", "--quarterly"), "financials/income/quarterly.table");
    }

    @Test
    void incomeTrailing() {
        cli.timeseries("AAPL", "timeseries/aapl-trailing-financials");
        Golden.assertMatches(cli.run("financials", "income", "AAPL", "--ttm"), "financials/income/ttm.table");
        Golden.assertMatches(
                cli.run("financials", "income", "AAPL", "--ttm", "--format", "csv"), "financials/income/ttm.csv");
    }

    @Test
    void incomeOfABank() {
        cli.timeseries("JPM", "timeseries/jpm-annual-financials");
        Golden.assertMatches(cli.run("financials", "income", "JPM"), "financials/income/bank.table");
    }

    @Test
    void incomeEmpty() {
        cli.timeseries("SPY", "timeseries/spy-annual-financials");
        Golden.assertMatches(cli.run("financials", "income", "SPY"), "financials/income/empty.table");
        Golden.assertMatches(
                cli.run("financials", "income", "SPY", "--format", "json"), "financials/income/empty.json");
        Golden.assertMatches(cli.run("financials", "income", "SPY", "--format", "csv"), "financials/income/empty.csv");
    }

    @Test
    void incomeError() {
        badRequest("AAPL");
        Golden.assertMatches(cli.run("financials", "income", "AAPL"), "financials/income/error.table");
        Golden.assertMatches(
                cli.run("financials", "income", "AAPL", "--format", "json"), "financials/income/error.json");
    }

    @Test
    void balanceAnnual() {
        cli.timeseries("AAPL", "timeseries/aapl-annual-balance-sheet");
        Golden.assertMatches(cli.run("financials", "balance", "AAPL"), "financials/balance/normal.table");
        Golden.assertMatches(
                cli.run("financials", "balance", "AAPL", "--format", "json"), "financials/balance/normal.json");
    }

    @Test
    void balanceOfABank() {
        cli.timeseries("JPM", "timeseries/jpm-annual-balance-sheet");
        Golden.assertMatches(cli.run("financials", "balance", "JPM"), "financials/balance/bank.table");
    }

    @Test
    void balanceEmpty() {
        cli.onGet(TIMESERIES + "SPY").respond(200, NO_SERIES);
        Golden.assertMatches(cli.run("financials", "balance", "SPY"), "financials/balance/empty.table");
        Golden.assertMatches(
                cli.run("financials", "balance", "SPY", "--format", "json"), "financials/balance/empty.json");
    }

    @Test
    void balanceError() {
        badRequest("AAPL");
        Golden.assertMatches(cli.run("financials", "balance", "AAPL"), "financials/balance/error.table");
    }

    @Test
    void balanceHasNoTrailingVariant() {
        CliResult result = cli.run("financials", "balance", "AAPL", "--ttm");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--ttm");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void cashFlowTrailing() {
        // the only cash flow recording is the trailing one, so it is the normal case
        cli.timeseries("AAPL", "timeseries/aapl-trailing-cash-flow");
        Golden.assertMatches(cli.run("financials", "cashflow", "AAPL", "--ttm"), "financials/cashflow/normal.table");
        Golden.assertMatches(
                cli.run("financials", "cashflow", "AAPL", "--ttm", "--format", "json"),
                "financials/cashflow/normal.json");
    }

    @Test
    void cashFlowEmpty() {
        cli.onGet(TIMESERIES + "SPY").respond(200, NO_SERIES);
        Golden.assertMatches(cli.run("financials", "cashflow", "SPY"), "financials/cashflow/empty.table");
        Golden.assertMatches(
                cli.run("financials", "cashflow", "SPY", "--format", "csv"), "financials/cashflow/empty.csv");
    }

    @Test
    void cashFlowError() {
        badRequest("AAPL");
        Golden.assertMatches(cli.run("financials", "cashflow", "AAPL"), "financials/cashflow/error.table");
    }

    @Test
    void requestsTheSelectedFrequency() {
        cli.timeseries("AAPL", "timeseries/aapl-quarterly-financials");
        assertThat(cli.run("financials", "income", "AAPL", "--quarterly").exitCode())
                .isZero();
        assertThat(cli.transport().requests().stream()
                        .filter(r -> r.uri().getPath().startsWith(TIMESERIES)))
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.uri().getRawQuery()).contains("type=quarterly"));
    }

    @Test
    void quarterlyAndTtmTogetherIsAUsageError() {
        for (String statement : new String[] {"income", "cashflow"}) {
            CliResult result = cli.run("financials", statement, "AAPL", "--quarterly", "--ttm");
            assertThat(result.exitCode()).as(statement).isEqualTo(2);
            assertThat(result.err()).as(statement).contains("mutually exclusive");
        }
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void missingStatementIsAUsageError() {
        CliResult result = cli.run("financials", "AAPL");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(cli.transport().requests()).isEmpty();
    }
}
