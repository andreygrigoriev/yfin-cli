package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code events} reads quoteSummary's {@code calendarEvents}; SPY has no fundamentals (404). */
class EventsCommandTest {
    private static final String QUOTE_SUMMARY = "/v10/finance/quoteSummary/";

    private final CliRunner cli = new CliRunner();

    @Test
    void normal() {
        cli.fixture(QUOTE_SUMMARY + "AAPL", "quoteSummary/analysis/aapl.json");
        Golden.assertMatches(cli.run("events", "AAPL"), "events/normal.table");
        Golden.assertMatches(cli.run("events", "AAPL", "--format", "json"), "events/normal.json");
        Golden.assertMatches(cli.run("events", "AAPL", "--format", "csv"), "events/normal.csv");
    }

    @Test
    void noCalendarIsEmpty() {
        cli.onGet(QUOTE_SUMMARY + "SPY").respond(404, Fixtures.load("quoteSummary/analysis/spy.json"));
        Golden.assertMatches(cli.run("events", "SPY"), "events/empty.table");
        Golden.assertMatches(cli.run("events", "SPY", "--format", "json"), "events/empty.json");
        Golden.assertMatches(cli.run("events", "SPY", "--format", "csv"), "events/empty.csv");
    }

    @Test
    void notFound() {
        cli.onGet(QUOTE_SUMMARY + "NOPE").respond(404, Fixtures.load("quoteSummary/not-found.json"));
        Golden.assertMatches(cli.run("events", "NOPE"), "events/not-found.table");
        Golden.assertMatches(cli.run("events", "NOPE", "--format", "json"), "events/not-found.json");
    }
}
