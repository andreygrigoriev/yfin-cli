package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.spi.HttpRequest;
import org.junit.jupiter.api.Test;

/**
 * {@code dividends}, {@code splits}, {@code capital-gains} and {@code actions} all read the full
 * daily chart (period {@code max}). The recorded AAPL window around August 2020 holds one dividend
 * and the 4:1 split; SCHD's weekly chart holds capital gains; AAPL's last month holds no events.
 */
class CorporateActionsCommandTest {
    private static final String CHART = "/v8/finance/chart/";

    private final CliRunner cli = new CliRunner();

    private void serve(String symbol, String fixture) {
        cli.fixture(CHART + symbol, "chart/" + fixture);
    }

    private void notFound() {
        cli.onGet(CHART + "NOPE").respond(404, Fixtures.load("chart/not-found.json"));
    }

    private void goldens(String command, String symbol, String name, String... formats) {
        Golden.assertMatches(cli.run(command, symbol), command + "/" + name + ".table");
        for (String format : formats) {
            Golden.assertMatches(cli.run(command, symbol, "--format", format), command + "/" + name + "." + format);
        }
    }

    @Test
    void dividends() {
        serve("AAPL", "aapl-split-dividend-2020.json");
        serve("MSFT", "aapl-1d-1mo.json");
        notFound();
        goldens("dividends", "AAPL", "normal", "json", "csv");
        goldens("dividends", "MSFT", "empty", "json", "csv");
        goldens("dividends", "NOPE", "not-found", "json");
    }

    @Test
    void splits() {
        serve("AAPL", "aapl-split-dividend-2020.json");
        serve("MSFT", "aapl-1d-1mo.json");
        notFound();
        goldens("splits", "AAPL", "normal", "json", "csv");
        goldens("splits", "MSFT", "empty", "json");
        goldens("splits", "NOPE", "not-found", "json");
    }

    @Test
    void capitalGains() {
        serve("SCHD", "schd-1wk-capital-gains.json");
        serve("AAPL", "aapl-1d-1mo.json");
        notFound();
        goldens("capital-gains", "SCHD", "normal", "json", "csv");
        goldens("capital-gains", "AAPL", "empty", "json");
        goldens("capital-gains", "NOPE", "not-found", "json");
    }

    @Test
    void actions() {
        serve("AAPL", "aapl-split-dividend-2020.json");
        serve("SCHD", "schd-1wk-capital-gains.json");
        serve("MSFT", "aapl-1d-1mo.json");
        notFound();
        goldens("actions", "AAPL", "normal", "json", "csv");
        goldens("actions", "SCHD", "capital-gains", "json");
        goldens("actions", "MSFT", "empty", "json", "csv");
        goldens("actions", "NOPE", "not-found", "json");
    }

    @Test
    void requestsTheFullDailyHistory() {
        serve("AAPL", "aapl-split-dividend-2020.json");
        assertThat(cli.run("dividends", "AAPL").exitCode()).isZero();
        HttpRequest request = cli.transport().requests().getLast();
        // the library spells "max" as a period1/period2 window, like yfinance
        assertThat(request.uri().getRawQuery())
                .contains("period1=")
                .contains("interval=1d")
                .contains("events=div%2Csplits%2CcapitalGains");
    }

    @Test
    void symbolIsRequired() {
        for (String command : new String[] {"dividends", "splits", "capital-gains", "actions"}) {
            CliResult result = cli.run(command);
            assertThat(result.exitCode()).as(command).isEqualTo(2);
            assertThat(result.err()).as(command).contains("Missing required parameter: 'SYMBOL'");
        }
    }
}
