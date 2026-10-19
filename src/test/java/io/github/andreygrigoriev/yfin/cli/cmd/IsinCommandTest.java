package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code isin} reads the short name from the quote endpoint, then searches it on the Business
 * Insider suggest service. An unknown symbol has no quote and so no ISIN: an empty record, exit 0.
 */
class IsinCommandTest {
    private static final String QUOTE = "/v7/finance/quote";
    private static final String SUGGEST = "/ajax/SearchController_Suggest";

    private final CliRunner cli = new CliRunner();

    @Test
    void normal() {
        cli.onGet(QUOTE).query("symbols", "AAPL").respond(200, Fixtures.load("quote/aapl.json"));
        cli.fixture(SUGGEST, "isin/aapl.txt");
        Golden.assertMatches(cli.run("isin", "AAPL"), "isin/normal.table");
        Golden.assertMatches(cli.run("isin", "AAPL", "--format", "json"), "isin/normal.json");
        Golden.assertMatches(cli.run("isin", "AAPL", "--format", "csv"), "isin/normal.csv");
    }

    @Test
    void noMatchIsEmpty() {
        // SAP.DE's short name is not among the suggestions
        cli.onGet(QUOTE).query("symbols", "SAP.DE").respond(200, Fixtures.load("quote/sap-de.json"));
        cli.fixture(SUGGEST, "isin/sap-de.txt");
        Golden.assertMatches(cli.run("isin", "SAP.DE"), "isin/empty.table");
        Golden.assertMatches(cli.run("isin", "SAP.DE", "--format", "json"), "isin/empty.json");
        Golden.assertMatches(cli.run("isin", "SAP.DE", "--format", "csv"), "isin/empty.csv");
    }

    @Test
    void unknownSymbolIsEmpty() {
        cli.onGet(QUOTE).respond(200, Fixtures.load("quote/not-found.json"));
        Golden.assertMatches(cli.run("isin", "NOPE", "--format", "json"), "isin/not-found.json");
    }

    @Test
    void suggestServiceFailing() {
        // synthetic: no recorded failure of the suggest service yet (docs/fixtures.md, gaps)
        cli.onGet(QUOTE).query("symbols", "AAPL").respond(200, Fixtures.load("quote/aapl.json"));
        cli.onGet(SUGGEST).respond(403, "forbidden");
        Golden.assertMatches(cli.run("isin", "AAPL"), "isin/error.table");
        Golden.assertMatches(cli.run("isin", "AAPL", "--format", "json"), "isin/error.json");
    }
}
