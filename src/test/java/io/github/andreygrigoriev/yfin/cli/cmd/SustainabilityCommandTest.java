package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code sustainability} reads quoteSummary's {@code esgScores}. Yahoo currently has no ESG data
 * for most symbols, so the normal case uses the library's synthetic MSFT body
 * ({@code msft-esg-synthetic}), shaped like Yahoo's former answer.
 */
class SustainabilityCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @Test
    void normal() {
        cli.fixture(QUOTE_SUMMARY + "MSFT", "quoteSummary/analysis/msft-esg-synthetic.json");
        Golden.assertMatches(cli.run("sustainability", "MSFT"), "sustainability/normal.table");
        Golden.assertMatches(cli.run("sustainability", "MSFT", "--format", "json"), "sustainability/normal.json");
    }

    @Test
    void noScoresIsEmpty() {
        cli.onGet(QUOTE_SUMMARY + "AAPL").respond(404, Fixtures.load("quoteSummary/analysis/aapl-esg.json"));
        Golden.assertMatches(cli.run("sustainability", "AAPL"), "sustainability/empty.table");
        Golden.assertMatches(cli.run("sustainability", "AAPL", "--format", "json"), "sustainability/empty.json");
        Golden.assertMatches(cli.run("sustainability", "AAPL", "--format", "csv"), "sustainability/empty.csv");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        Golden.assertMatches(cli.run("sustainability", "NOPE"), "sustainability/not-found.table");
        Golden.assertMatches(cli.run("sustainability", "NOPE", "--format", "json"), "sustainability/not-found.json");
    }
}
