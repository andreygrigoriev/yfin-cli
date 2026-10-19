package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.spi.HttpRequest;
import io.github.andreygrigoriev.yfin.testing.FakeTransport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code news} posts to Yahoo's news stream ({@code /xhr/ncp}). The library's stream fixtures are
 * synthetic ({@code news/*-ncp-synthetic}): as of October 2026 the endpoint answers 404 and the
 * library falls back to the news of {@code /v1/finance/search}, which the multi-symbol cases use.
 */
class NewsCommandTest {
    private static final String NCP = "/xhr/ncp";
    private static final String SEARCH = "/v1/finance/search";

    private final CliRunner cli = new CliRunner();

    @Test
    void stream() {
        cli.onPost(NCP).respond(200, Fixtures.load("news/aapl-ncp-synthetic.json"));
        Golden.assertMatches(cli.run("news", "AAPL"), "news/normal.table");
        Golden.assertMatches(cli.run("news", "AAPL", "--format", "json"), "news/normal.json");
        Golden.assertMatches(cli.run("news", "AAPL", "--format", "csv"), "news/normal.csv");
    }

    @Test
    void countAndTabReachTheRequest() {
        cli.onPost(NCP).respond(200, Fixtures.load("news/empty-ncp-synthetic.json"));
        assertThat(cli.run("news", "AAPL", "--count", "25", "--tab", "press-releases")
                        .exitCode())
                .isZero();
        HttpRequest request = cli.transport().requests().stream()
                .filter(r -> r.uri().getPath().equals(NCP))
                .findFirst()
                .orElseThrow();
        Map<String, List<String>> q = FakeTransport.parseQuery(request.uri().getRawQuery());
        assertThat(q).containsEntry("queryRef", List.of("pressRelease"));
        assertThat(request.bodyAsString().orElseThrow()).contains("\"snippetCount\":25");
    }

    @Test
    void searchFallbackForSeveralSymbols() {
        cli.onPost(NCP).respond(404, Fixtures.load("news/not-found.json"));
        cli.onGet(SEARCH).query("q", "AAPL").respond(200, Fixtures.load("news/search-aapl.json"));
        cli.onGet(SEARCH).query("q", "ZZZZNOTREAL").respond(200, Fixtures.load("news/search-unknown.json"));
        Golden.assertMatches(cli.run("news", "AAPL", "ZZZZNOTREAL", "--count", "3"), "news/multi.table");
        Golden.assertMatches(
                cli.run("news", "AAPL", "ZZZZNOTREAL", "--count", "3", "--format", "json"), "news/multi.json");
    }

    @Test
    void empty() {
        cli.onPost(NCP).respond(200, Fixtures.load("news/empty-ncp-synthetic.json"));
        Golden.assertMatches(cli.run("news", "AAPL"), "news/empty.table");
        Golden.assertMatches(cli.run("news", "AAPL", "--format", "json"), "news/empty.json");
        Golden.assertMatches(cli.run("news", "AAPL", "--format", "csv"), "news/empty.csv");
    }

    @Test
    void badRequestFailsOnlyThatSymbol() {
        cli.onPost(NCP).respond(404, Fixtures.load("news/not-found.json"));
        cli.onGet(SEARCH).query("q", "AAPL").respond(200, Fixtures.load("news/search-aapl.json"));
        cli.onGet(SEARCH).query("q", "MSFT").respond(400, Fixtures.load("search/bad-request.json"));
        Golden.assertMatches(cli.run("news", "AAPL", "MSFT", "--count", "1"), "news/bad-request.table");
        Golden.assertMatches(
                cli.run("news", "AAPL", "MSFT", "--count", "1", "--format", "json"), "news/bad-request.json");
    }

    @Test
    void countMustBePositive() {
        CliResult result = cli.run("news", "AAPL", "--count", "0");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--count must be at least 1");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void unknownTabIsUsageError() {
        CliResult result = cli.run("news", "AAPL", "--tab", "gossip");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("news, all, press-releases");
    }
}
