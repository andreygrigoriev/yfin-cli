package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code holders} reads quoteSummary's holder modules, all from one recording per symbol. Yahoo
 * answers SPY with a 404 "No fundamentals data found", which the library treats as no data; SAP.DE
 * has institutional and fund holders but no insider modules.
 */
class HoldersCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @Test
    void major() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/holders/aapl.json");
        Golden.assertMatches(cli.run("holders", "major", "AAPL"), "holders/major/normal.table");
        Golden.assertMatches(cli.run("holders", "major", "AAPL", "--format", "json"), "holders/major/normal.json");
        Golden.assertMatches(cli.run("holders", "major", "AAPL", "--format", "csv"), "holders/major/normal.csv");
    }

    @Test
    void institutional() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/holders/aapl.json");
        Golden.assertMatches(cli.run("holders", "institutional", "AAPL"), "holders/institutional/normal.table");
        Golden.assertMatches(
                cli.run("holders", "institutional", "AAPL", "--format", "json"), "holders/institutional/normal.json");
        Golden.assertMatches(
                cli.run("holders", "institutional", "AAPL", "--format", "csv"), "holders/institutional/normal.csv");
    }

    @Test
    void funds() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/holders/aapl.json");
        Golden.assertMatches(cli.run("holders", "funds", "AAPL"), "holders/funds/normal.table");
        Golden.assertMatches(cli.run("holders", "funds", "AAPL", "--format", "json"), "holders/funds/normal.json");
        cli.fixture(QUOTE_SUMMARY + "SAP.DE", "quoteSummary/holders/sap-de.json");
        Golden.assertMatches(cli.run("holders", "funds", "SAP.DE"), "holders/funds/sap-de.table");
    }

    @Test
    void insiders() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/holders/aapl.json");
        Golden.assertMatches(cli.run("holders", "insiders", "AAPL"), "holders/insiders/normal.table");
        Golden.assertMatches(
                cli.run("holders", "insiders", "AAPL", "--format", "json"), "holders/insiders/normal.json");
    }

    @Test
    void insiderTransactions() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/holders/aapl.json");
        Golden.assertMatches(cli.run("holders", "insider-tx", "AAPL"), "holders/insider-tx/normal.table");
        Golden.assertMatches(
                cli.run("holders", "insider-tx", "AAPL", "--format", "json"), "holders/insider-tx/normal.json");
    }

    @Test
    void noInsiderModulesIsEmpty() {
        cli.fixture(QUOTE_SUMMARY + "SAP.DE", "quoteSummary/holders/sap-de.json");
        Golden.assertMatches(cli.run("holders", "insiders", "SAP.DE"), "holders/insiders/sap-de.table");
        Golden.assertMatches(
                cli.run("holders", "insider-tx", "SAP.DE", "--format", "json"), "holders/insider-tx/sap-de.json");
    }

    @Test
    void noFundamentalsIsEmpty() {
        cli.onGet(QUOTE_SUMMARY + "SPY").respond(404, Fixtures.load("quoteSummary/holders/spy.json"));
        for (String holders : new String[] {"major", "institutional", "funds", "insiders", "insider-tx"}) {
            String dir = "holders/" + holders + "/";
            Golden.assertMatches(cli.run("holders", holders, "SPY"), dir + "empty.table");
            Golden.assertMatches(cli.run("holders", holders, "SPY", "--format", "json"), dir + "empty.json");
            Golden.assertMatches(cli.run("holders", holders, "SPY", "--format", "csv"), dir + "empty.csv");
        }
    }

    @Test
    void cryptoIsEmpty() {
        cli.onGet(QUOTE_SUMMARY + "BTC-USD").respond(404, Fixtures.load("quoteSummary/holders/btc-usd.json"));
        Golden.assertMatches(cli.run("holders", "major", "BTC-USD", "--format", "json"), "holders/major/empty.json");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        for (String holders : new String[] {"major", "institutional", "funds", "insiders", "insider-tx"}) {
            String dir = "holders/" + holders + "/";
            Golden.assertMatches(cli.run("holders", holders, "NOPE"), dir + "not-found.table");
            Golden.assertMatches(cli.run("holders", holders, "NOPE", "--format", "json"), dir + "not-found.json");
        }
    }

    @Test
    void missingSubcommandIsUsageError() {
        CliResult result = cli.run("holders");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Missing required subcommand");
    }
}
