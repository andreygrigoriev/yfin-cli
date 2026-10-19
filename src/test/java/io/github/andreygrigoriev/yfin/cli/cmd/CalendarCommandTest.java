package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code calendar} POSTs a query to the visualization endpoint. By default the earnings calendar
 * first reads today's most active stocks from a predefined screen; {@code --all} skips that. Yahoo's
 * error envelope ({@code error}) is a data error.
 */
class CalendarCommandTest {
    private static final String VISUALIZATION = "/v1/finance/visualization";
    private static final String SCREENER = "/v1/finance/screener/predefined/saved";
    private static final String[] WEEK = {"--from", "2026-10-14", "--to", "2026-10-21"};

    private final CliRunner cli = new CliRunner();

    @Test
    void earnings() {
        cli.fixture(SCREENER, "calendar/most-actives.json");
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/earnings-week.json"));
        Golden.assertMatches(cli.run(args("earnings")), "calendar/earnings/normal.table");
        Golden.assertMatches(cli.run(args("earnings", "--format", "json")), "calendar/earnings/normal.json");
        Golden.assertMatches(cli.run(args("earnings", "--format", "csv")), "calendar/earnings/normal.csv");
    }

    @Test
    void earningsAllWithMarketCap() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/earnings-week-mcap.json"));
        Golden.assertMatches(
                cli.run(args("earnings", "--all", "--market-cap", "1e10")), "calendar/earnings/market-cap.table");
        // no most-active lookup
        assertThat(cli.transport().requests()).noneMatch(r -> r.uri().getPath().equals(SCREENER));
        assertThat(body()).contains("1.0E10").contains("\"size\":12");
    }

    @Test
    void earningsEmpty() {
        cli.fixture(SCREENER, "calendar/most-actives.json");
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/earnings-empty.json"));
        Golden.assertMatches(cli.run(args("earnings")), "calendar/earnings/empty.table");
        Golden.assertMatches(cli.run(args("earnings", "--format", "json")), "calendar/earnings/empty.json");
        Golden.assertMatches(cli.run(args("earnings", "--format", "csv")), "calendar/earnings/empty.csv");
    }

    @Test
    void earningsError() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/error.json"));
        Golden.assertMatches(cli.run(args("earnings", "--all")), "calendar/earnings/error.table");
        Golden.assertMatches(cli.run(args("earnings", "--all", "--format", "json")), "calendar/earnings/error.json");
    }

    @Test
    void ipo() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/ipo-week.json"));
        Golden.assertMatches(cli.run(args("ipo")), "calendar/ipo/normal.table");
        Golden.assertMatches(cli.run(args("ipo", "--format", "json")), "calendar/ipo/normal.json");
        Golden.assertMatches(cli.run(args("ipo", "--format", "csv")), "calendar/ipo/normal.csv");
    }

    @Test
    void ipoEmpty() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/ipo-empty.json"));
        Golden.assertMatches(cli.run(args("ipo")), "calendar/ipo/empty.table");
        Golden.assertMatches(cli.run(args("ipo", "--format", "json")), "calendar/ipo/empty.json");
    }

    @Test
    void ipoError() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/error.json"));
        Golden.assertMatches(cli.run(args("ipo")), "calendar/ipo/error.table");
        Golden.assertMatches(cli.run(args("ipo", "--format", "json")), "calendar/ipo/error.json");
    }

    @Test
    void splits() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/splits-week.json"));
        Golden.assertMatches(cli.run(args("splits", "--limit", "100")), "calendar/splits/normal.table");
        Golden.assertMatches(
                cli.run(args("splits", "--limit", "100", "--format", "json")), "calendar/splits/normal.json");
        Golden.assertMatches(
                cli.run(args("splits", "--limit", "100", "--format", "csv")), "calendar/splits/normal.csv");
        assertThat(body()).contains("\"2026-10-14\"").contains("\"2026-10-21\"").contains("\"size\":100");
    }

    @Test
    void splitsEmpty() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/splits-empty.json"));
        Golden.assertMatches(cli.run(args("splits")), "calendar/splits/empty.table");
        Golden.assertMatches(cli.run(args("splits", "--format", "json")), "calendar/splits/empty.json");
    }

    @Test
    void splitsError() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/error.json"));
        Golden.assertMatches(cli.run(args("splits")), "calendar/splits/error.table");
        Golden.assertMatches(cli.run(args("splits", "--format", "json")), "calendar/splits/error.json");
    }

    @Test
    void defaultWindowIsTodayPlusSevenDays() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/ipo-empty.json"));
        LocalDate today = LocalDate.now();
        assertThat(cli.run("calendar", "ipo").exitCode()).isZero();
        // a run across midnight may see the next day
        LocalDate start = body().contains("\"" + today + "\"") ? today : today.plusDays(1);
        assertThat(body()).contains("\"" + start + "\"").contains("\"" + start.plusDays(7) + "\"");
    }

    @Test
    void toAloneIsAWindowFromToday() {
        cli.onPost(VISUALIZATION).respond(200, Fixtures.load("calendar/ipo-empty.json"));
        LocalDate to = LocalDate.now().plusDays(30);
        assertThat(cli.run("calendar", "ipo", "--to", to.toString()).exitCode()).isZero();
        assertThat(body()).contains("\"" + to + "\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"earnings", "ipo", "splits"})
    void toBeforeFromIsUsageError(String type) {
        CliResult result = cli.run("calendar", type, "--from", "2026-10-21", "--to", "2026-10-14");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--to 2026-10-14 is before --from 2026-10-21");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101"})
    void limitOutOfRangeIsUsageError(String limit) {
        CliResult result = cli.run(args("splits", "--limit", limit));
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--limit must be in 1..100, was " + limit);
        assertThat(cli.transport().requests()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "Infinity", "NaN"})
    void nonPositiveMarketCapIsUsageError(String marketCap) {
        CliResult result = cli.run(args("earnings", "--market-cap", marketCap));
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--market-cap must be positive");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void missingSubcommandIsUsageError() {
        CliResult result = cli.run("calendar");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("Missing required subcommand");
    }

    private static String[] args(String type, String... extra) {
        String[] args = new String[2 + WEEK.length + extra.length];
        args[0] = "calendar";
        args[1] = type;
        System.arraycopy(WEEK, 0, args, 2, WEEK.length);
        System.arraycopy(extra, 0, args, 2 + WEEK.length, extra.length);
        return args;
    }

    private String body() {
        return cli.transport().requests().stream()
                .filter(r -> r.uri().getPath().equals(VISUALIZATION))
                .reduce((a, b) -> b)
                .orElseThrow()
                .bodyAsString()
                .orElseThrow();
    }
}
