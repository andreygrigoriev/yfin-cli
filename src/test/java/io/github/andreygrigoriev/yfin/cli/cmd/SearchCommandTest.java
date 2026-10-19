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

/** {@code search} reads {@code /v1/finance/search}: quotes, plus news with {@code --news N}. */
class SearchCommandTest {
    private static final String SEARCH = "/v1/finance/search";

    private final CliRunner cli = new CliRunner();

    @Test
    void quotes() {
        cli.fixture(SEARCH, "search/apple.json");
        Golden.assertMatches(cli.run("search", "apple"), "search/normal.table");
        Golden.assertMatches(cli.run("search", "apple", "--format", "json"), "search/normal.json");
        Golden.assertMatches(cli.run("search", "apple", "--format", "csv"), "search/normal.csv");
    }

    @Test
    void quotesAndNews() {
        cli.fixture(SEARCH, "search/tech.json");
        Golden.assertMatches(cli.run("search", "tech", "--news", "3"), "search/news.table");
        Golden.assertMatches(cli.run("search", "tech", "--news", "3", "--format", "json"), "search/news.json");
        Golden.assertMatches(cli.run("search", "tech", "--news", "3", "--format", "csv"), "search/news.csv");
    }

    @Test
    void maxAndNewsReachTheRequest() {
        cli.fixture(SEARCH, "search/empty.json");
        assertThat(cli.run("search", "apple", "--max", "3", "--news", "2").exitCode())
                .isZero();
        Map<String, List<String>> q = FakeTransport.parseQuery(cli.transport().requests().stream()
                .filter(r -> r.uri().getPath().equals(SEARCH))
                .findFirst()
                .orElseThrow()
                .uri()
                .getRawQuery());
        assertThat(q)
                .containsEntry("q", List.of("apple"))
                .containsEntry("quotesCount", List.of("3"))
                .containsEntry("newsCount", List.of("2"));
    }

    @Test
    void empty() {
        cli.fixture(SEARCH, "search/empty.json");
        Golden.assertMatches(cli.run("search", "zzzzqqq"), "search/empty.table");
        Golden.assertMatches(cli.run("search", "zzzzqqq", "--format", "json"), "search/empty.json");
        Golden.assertMatches(cli.run("search", "zzzzqqq", "--news", "2", "--format", "json"), "search/empty-news.json");
        Golden.assertMatches(cli.run("search", "zzzzqqq", "--news", "2", "--format", "csv"), "search/empty-news.csv");
    }

    @Test
    void badRequest() {
        cli.onGet(SEARCH).respond(400, Fixtures.load("search/bad-request.json"));
        Golden.assertMatches(cli.run("search", "apple"), "search/bad-request.table");
        Golden.assertMatches(cli.run("search", "apple", "--format", "json"), "search/bad-request.json");
    }

    @Test
    void negativeCountsAndBlankQueryAreUsageErrors() {
        for (String[] args : List.of(
                new String[] {"search", "apple", "--max", "-1"},
                new String[] {"search", "apple", "--news", "-1"},
                new String[] {"search", " "})) {
            CliResult result = cli.run(args);
            assertThat(result.exitCode()).as(String.join(" ", args)).isEqualTo(2);
        }
        assertThat(cli.transport().requests()).isEmpty();
    }
}
