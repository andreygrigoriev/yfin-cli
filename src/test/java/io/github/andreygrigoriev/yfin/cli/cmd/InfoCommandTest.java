package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code info} reads quoteSummary, the quote endpoint and, for symbols with fundamentals, the
 * trailing PEG ratio from the timeseries endpoint. BTC-USD is the sparse ("empty") case: no
 * fundamentals, so most fields are missing.
 */
class InfoCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";
    private static final String QUOTE = "/v7/finance/quote";
    private static final String TIMESERIES = "/ws/fundamentals-timeseries/v1/finance/timeseries/";

    private final CliRunner cli = new CliRunner();

    private void serve(String symbol, String fixture, String peg) {
        cli.fixture(QUOTE_SUMMARY + symbol, "quoteSummary/" + fixture);
        cli.onGet(QUOTE).query("symbols", symbol).respond(200, Fixtures.load("quote/" + fixture));
        cli.fixture(TIMESERIES + symbol, "timeseries/peg/" + peg);
    }

    @Test
    void normal() {
        serve("AAPL", "aapl.json", "aapl.json");
        Golden.assertMatches(cli.run("info", "AAPL"), "info/normal.table");
        Golden.assertMatches(cli.run("info", "AAPL", "--format", "json"), "info/normal.json");
        Golden.assertMatches(cli.run("info", "AAPL", "--format", "csv"), "info/normal.csv");
    }

    @Test
    void sparse() {
        serve("BTC-USD", "btc-usd.json", "spy.json");
        Golden.assertMatches(cli.run("info", "BTC-USD"), "info/empty.table");
        Golden.assertMatches(cli.run("info", "BTC-USD", "--format", "json"), "info/empty.json");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        cli.onGet(QUOTE).query("symbols", "NOPE").respond(200, Fixtures.load("quote/not-found.json"));
        Golden.assertMatches(cli.run("info", "NOPE"), "info/not-found.table");
        Golden.assertMatches(cli.run("info", "NOPE", "--format", "json"), "info/not-found.json");
    }

    @Test
    void csvIsOneFieldValueLinePerField() {
        serve("AAPL", "aapl.json", "aapl.json");
        CliResult result = cli.run("info", "AAPL", "--format", "csv");

        assertThat(result.exitCode()).isZero();
        List<String> lines = result.out().lines().toList();
        assertThat(lines.getFirst()).isEqualTo("field,value");
        assertThat(lines).hasSize(1 + Specs.TICKER_INFO.columns().size());
        assertThat(lines).contains("symbol,AAPL", "quote_type,EQUITY", "sector,Technology");
    }

    @Test
    void symbolIsRequired() {
        CliResult result = cli.run("info");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Missing required parameter: 'SYMBOL'");
    }

    @Test
    void blankSymbolIsAUsageError() {
        CliResult result = cli.run("info", " ");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Invalid symbol");
    }
}
