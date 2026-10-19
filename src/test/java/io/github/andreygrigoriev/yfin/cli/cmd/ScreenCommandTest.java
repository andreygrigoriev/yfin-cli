package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.testing.FakeTransport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code screen} reads {@code /v1/finance/screener/predefined/saved}; with {@code --offset} the
 * library runs the screen's equivalent custom query, a POST to {@code /v1/finance/screener}.
 */
class ScreenCommandTest {
    private static final String SCREENER = "/v1/finance/screener";
    private static final String PREDEFINED = SCREENER + "/predefined/saved";

    private final CliRunner cli = new CliRunner();

    @Test
    void predefined() {
        cli.fixture(PREDEFINED, "screener/predefined-day-gainers.json");
        Golden.assertMatches(cli.run("screen", "day_gainers"), "screen/normal.table");
        Golden.assertMatches(cli.run("screen", "day_gainers", "--format", "json"), "screen/normal.json");
        Golden.assertMatches(cli.run("screen", "day_gainers", "--format", "csv"), "screen/normal.csv");
    }

    @Test
    void sizeAndSortReachTheRequest() {
        cli.fixture(PREDEFINED, "screener/predefined-most-actives.json");
        Golden.assertMatches(
                cli.run("screen", "most_actives", "--size", "5", "--sort", "dayvolume", "--asc"),
                "screen/most-actives.table");
        Map<String, List<String>> q = FakeTransport.parseQuery(cli.transport().requests().stream()
                .filter(r -> r.uri().getPath().equals(PREDEFINED))
                .findFirst()
                .orElseThrow()
                .uri()
                .getRawQuery());
        assertThat(q)
                .containsEntry("scrIds", List.of("most_actives"))
                .containsEntry("count", List.of("5"))
                .containsEntry("sortField", List.of("dayvolume"))
                .containsEntry("sortType", List.of("ASC"));
    }

    @Test
    void offsetRunsTheCustomQuery() {
        cli.onPost(SCREENER).respond(200, Fixtures.load("screener/predefined-offset.json"));
        Golden.assertMatches(
                cli.run("screen", "day_gainers", "--offset", "5", "--size", "3", "--format", "json"),
                "screen/offset.json");
    }

    @Test
    void list() {
        Golden.assertMatches(cli.run("screen", "--list"), "screen/list.table");
        Golden.assertMatches(cli.run("screen", "--list", "--format", "json"), "screen/list.json");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void empty() {
        // custom-empty is a recorded custom screen with no matches; --offset sends day_gainers there
        cli.onPost(SCREENER).respond(200, Fixtures.load("screener/custom-empty.json"));
        Golden.assertMatches(cli.run("screen", "day_gainers", "--offset", "1000"), "screen/empty.table");
        Golden.assertMatches(
                cli.run("screen", "day_gainers", "--offset", "1000", "--format", "json"), "screen/empty.json");
        Golden.assertMatches(
                cli.run("screen", "day_gainers", "--offset", "1000", "--format", "csv"), "screen/empty.csv");
    }

    @Test
    void notFound() {
        cli.onGet(PREDEFINED).respond(404, Fixtures.load("screener/predefined-unknown.json"));
        Golden.assertMatches(cli.run("screen", "day_gainers"), "screen/not-found.table");
        Golden.assertMatches(cli.run("screen", "day_gainers", "--format", "json"), "screen/not-found.json");
    }

    @Test
    void badRequest() {
        cli.onPost(SCREENER).respond(400, Fixtures.load("screener/custom-bad-request.json"));
        Golden.assertMatches(cli.run("screen", "day_gainers", "--offset", "5"), "screen/bad-request.table");
    }

    @Test
    void unknownName() {
        Golden.assertMatches(cli.run("screen", "no_such_screen"), "screen/unknown.table");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void usageErrors() {
        for (String[] args : List.of(
                new String[] {"screen"},
                new String[] {"screen", "day_gainers", "--list"},
                new String[] {"screen", "day_gainers", "--size", "0"},
                new String[] {"screen", "day_gainers", "--size", "251"},
                new String[] {"screen", "day_gainers", "--offset", "-1"},
                new String[] {"screen", "day_gainers", "--asc"},
                new String[] {"screen", "day_gainers", "--sort", " "})) {
            CliResult result = cli.run(args);
            assertThat(result.exitCode()).as(String.join(" ", args)).isEqualTo(2);
        }
        assertThat(cli.transport().requests()).isEmpty();
    }
}
