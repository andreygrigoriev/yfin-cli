package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code recommendations}, {@code upgrades} and {@code price-target} read quoteSummary's {@code
 * recommendationTrend}, {@code upgradeDowngradeHistory} and {@code financialData}. SPY has no
 * fundamentals (Yahoo's 404 is no data); SAP.DE's recording has no rating changes.
 */
class AnalystCommandsTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    private void aapl() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/analysis/aapl.json");
    }

    private void spy() {
        cli.onGet(QUOTE_SUMMARY + "SPY").respond(404, Fixtures.load("quoteSummary/analysis/spy.json"));
    }

    private void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
    }

    @Test
    void recommendations() {
        aapl();
        Golden.assertMatches(cli.run("recommendations", "AAPL"), "recommendations/normal.table");
        Golden.assertMatches(cli.run("recommendations", "AAPL", "--format", "json"), "recommendations/normal.json");
        Golden.assertMatches(cli.run("recommendations", "AAPL", "--format", "csv"), "recommendations/normal.csv");
        // yfinance's recommendations_summary is the same table
        assertThat(cli.run("recommendations", "AAPL", "--summary")).isEqualTo(cli.run("recommendations", "AAPL"));
    }

    @Test
    void recommendationsEmptyAndNotFound() {
        spy();
        notFound();
        Golden.assertMatches(cli.run("recommendations", "SPY"), "recommendations/empty.table");
        Golden.assertMatches(cli.run("recommendations", "SPY", "--format", "json"), "recommendations/empty.json");
        Golden.assertMatches(cli.run("recommendations", "NOPE"), "recommendations/not-found.table");
    }

    @Test
    void upgrades() {
        aapl();
        Golden.assertMatches(cli.run("upgrades", "AAPL"), "upgrades/normal.table");
        Golden.assertMatches(cli.run("upgrades", "AAPL", "--format", "json"), "upgrades/normal.json");
        Golden.assertMatches(cli.run("upgrades", "AAPL", "--format", "csv"), "upgrades/normal.csv");
    }

    @Test
    void upgradesEmptyAndNotFound() {
        cli.fixture(QUOTE_SUMMARY + "SAP.DE", "quoteSummary/analysis/sap-de.json");
        notFound();
        Golden.assertMatches(cli.run("upgrades", "SAP.DE"), "upgrades/empty.table");
        Golden.assertMatches(cli.run("upgrades", "SAP.DE", "--format", "json"), "upgrades/empty.json");
        Golden.assertMatches(cli.run("upgrades", "NOPE"), "upgrades/not-found.table");
    }

    @Test
    void priceTarget() {
        aapl();
        Golden.assertMatches(cli.run("price-target", "AAPL"), "price-target/normal.table");
        Golden.assertMatches(cli.run("price-target", "AAPL", "--format", "json"), "price-target/normal.json");
        Golden.assertMatches(cli.run("price-target", "AAPL", "--format", "csv"), "price-target/normal.csv");
    }

    @Test
    void priceTargetEmptyAndNotFound() {
        spy();
        notFound();
        Golden.assertMatches(cli.run("price-target", "SPY"), "price-target/empty.table");
        Golden.assertMatches(cli.run("price-target", "SPY", "--format", "json"), "price-target/empty.json");
        Golden.assertMatches(cli.run("price-target", "SPY", "--format", "csv"), "price-target/empty.csv");
        Golden.assertMatches(cli.run("price-target", "NOPE", "--format", "json"), "price-target/not-found.json");
    }

    @Test
    void blankSymbolIsUsageError() {
        CliResult result = cli.run("upgrades", " ");
        assertThat(result.exitCode()).isEqualTo(2);
    }
}
