package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code fund} reads quoteSummary's fund modules; AAPL is not a fund, so it prints nothing. */
class FundCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @Test
    void etf() {
        cli.fixture(QUOTE_SUMMARY + "SPY", "quoteSummary/funds/spy.json");
        Golden.assertMatches(cli.run("fund", "SPY"), "fund/normal.table");
        Golden.assertMatches(cli.run("fund", "SPY", "--format", "json"), "fund/normal.json");
        Golden.assertMatches(cli.run("fund", "SPY", "--format", "csv"), "fund/normal.csv");
        Golden.assertMatches(cli.run("fund", "SPY", "--holdings"), "fund/holdings.table");
        Golden.assertMatches(cli.run("fund", "SPY", "--holdings", "--format", "csv"), "fund/holdings.csv");
        Golden.assertMatches(cli.run("fund", "SPY", "--sectors"), "fund/sectors.table");
        Golden.assertMatches(cli.run("fund", "SPY", "--sectors", "--format", "json"), "fund/sectors.json");
    }

    @Test
    void mutualFund() {
        cli.fixture(QUOTE_SUMMARY + "AGTHX", "quoteSummary/funds/agthx.json");
        Golden.assertMatches(cli.run("fund", "AGTHX"), "fund/mutual-fund.table");
        Golden.assertMatches(cli.run("fund", "AGTHX", "--format", "json"), "fund/mutual-fund.json");
    }

    @Test
    void notAFundIsEmpty() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/funds/aapl.json");
        Golden.assertMatches(cli.run("fund", "AAPL"), "fund/empty.table");
        Golden.assertMatches(cli.run("fund", "AAPL", "--format", "json"), "fund/empty.json");
        Golden.assertMatches(cli.run("fund", "AAPL", "--format", "csv"), "fund/empty.csv");
        Golden.assertMatches(cli.run("fund", "AAPL", "--holdings", "--format", "json"), "fund/empty-holdings.json");
        Golden.assertMatches(cli.run("fund", "AAPL", "--sectors", "--format", "csv"), "fund/empty-sectors.csv");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        Golden.assertMatches(cli.run("fund", "NOPE"), "fund/not-found.table");
        Golden.assertMatches(cli.run("fund", "NOPE", "--format", "json"), "fund/not-found.json");
    }

    @Test
    void holdingsAndSectorsAreExclusive() {
        CliResult result = cli.run("fund", "SPY", "--holdings", "--sectors");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("mutually exclusive");
    }
}
