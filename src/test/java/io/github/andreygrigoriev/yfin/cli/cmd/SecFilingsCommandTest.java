package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code sec-filings} reads the quoteSummary {@code secFilings} module; Yahoo has none for SPY. */
class SecFilingsCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @Test
    void normal() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/secfilings/aapl.json");
        Golden.assertMatches(cli.run("sec-filings", "AAPL"), "sec-filings/normal.table");
        Golden.assertMatches(cli.run("sec-filings", "AAPL", "--format", "json"), "sec-filings/normal.json");
        Golden.assertMatches(cli.run("sec-filings", "AAPL", "--format", "csv"), "sec-filings/normal.csv");
    }

    @Test
    void empty() {
        cli.onGet(QUOTE_SUMMARY + "SPY").respond(404, Fixtures.load("quoteSummary/secfilings/spy.json"));
        Golden.assertMatches(cli.run("sec-filings", "SPY"), "sec-filings/empty.table");
        Golden.assertMatches(cli.run("sec-filings", "SPY", "--format", "json"), "sec-filings/empty.json");
        Golden.assertMatches(cli.run("sec-filings", "SPY", "--format", "csv"), "sec-filings/empty.csv");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        Golden.assertMatches(cli.run("sec-filings", "NOPE"), "sec-filings/not-found.table");
        Golden.assertMatches(cli.run("sec-filings", "NOPE", "--format", "json"), "sec-filings/not-found.json");
    }

    @Test
    void requestsTheSecFilingsModule() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/secfilings/aapl.json");
        CliResult result = cli.run("sec-filings", "AAPL");
        assertThat(result.exitCode()).isZero();
        assertThat(cli.transport().requests().getLast().uri().getRawQuery()).contains("modules=secFilings");
    }
}
