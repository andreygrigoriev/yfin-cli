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
 * {@code estimates} reads quoteSummary's trend modules ({@code earningsTrend}, {@code
 * earningsHistory}, and the industry, sector and index trends for {@code growth}), all from one
 * recording per symbol. Yahoo answers SPY with a 404 "No fundamentals data found", which the
 * library treats as no data.
 */
class EstimatesCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @ParameterizedTest
    @ValueSource(strings = {"earnings", "revenue", "history", "eps-trend", "eps-revisions", "growth"})
    void normal(String estimates) {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/analysis/aapl.json");
        String dir = "estimates/" + estimates + "/";
        Golden.assertMatches(cli.run("estimates", estimates, "AAPL"), dir + "normal.table");
        Golden.assertMatches(cli.run("estimates", estimates, "AAPL", "--format", "json"), dir + "normal.json");
        Golden.assertMatches(cli.run("estimates", estimates, "AAPL", "--format", "csv"), dir + "normal.csv");
    }

    @Test
    void otherCurrency() {
        cli.fixture(QUOTE_SUMMARY + "SAP.DE", "quoteSummary/analysis/sap-de.json");
        Golden.assertMatches(cli.run("estimates", "earnings", "SAP.DE"), "estimates/earnings/sap-de.table");
    }

    @ParameterizedTest
    @ValueSource(strings = {"earnings", "revenue", "history", "eps-trend", "eps-revisions", "growth"})
    void noFundamentalsIsEmpty(String estimates) {
        cli.onGet(QUOTE_SUMMARY + "SPY").respond(404, Fixtures.load("quoteSummary/analysis/spy.json"));
        String dir = "estimates/" + estimates + "/";
        Golden.assertMatches(cli.run("estimates", estimates, "SPY"), dir + "empty.table");
        Golden.assertMatches(cli.run("estimates", estimates, "SPY", "--format", "json"), dir + "empty.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"earnings", "revenue", "history", "eps-trend", "eps-revisions", "growth"})
    void notFound(String estimates) {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        String dir = "estimates/" + estimates + "/";
        Golden.assertMatches(cli.run("estimates", estimates, "NOPE"), dir + "not-found.table");
        Golden.assertMatches(cli.run("estimates", estimates, "NOPE", "--format", "json"), dir + "not-found.json");
    }

    @Test
    void missingSubcommandIsUsageError() {
        CliResult result = cli.run("estimates");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Missing required subcommand");
    }
}
