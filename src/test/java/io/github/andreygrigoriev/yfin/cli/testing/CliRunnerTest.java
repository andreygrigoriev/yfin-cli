package io.github.andreygrigoriev.yfin.cli.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andreygrigoriev.yfin.spi.HttpRequest;
import io.github.andreygrigoriev.yfin.spi.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CliRunnerTest {
    private final CliRunner cli = new CliRunner();

    @Test
    void capturesStdoutAndExitCode() {
        CliResult result = cli.run("--version");
        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).startsWith("yfin ").contains("yfin-core ");
        assertThat(result.err()).isEmpty();
    }

    @Test
    void capturesStderrOfUsageErrors() {
        CliResult result = cli.run("--bogus");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.out()).isEmpty();
        assertThat(result.err()).contains("Unknown option: '--bogus'");
    }

    @Test
    void eachRunStartsFresh() {
        assertThat(cli.run("--bogus").exitCode()).isEqualTo(2);
        CliResult second = cli.run("--version");
        assertThat(second.exitCode()).isZero();
        assertThat(second.err()).isEmpty();
    }

    @Test
    void registersTheCookieAndCrumbBootstrap() {
        assertThat(body(send("https://fc.yahoo.com"))).isEqualTo("not found");
        assertThat(send("https://fc.yahoo.com").status()).isEqualTo(404);
        assertThat(body(send("https://query1.finance.yahoo.com/v1/test/getcrumb")))
                .isEqualTo("crumb1");
    }

    @Test
    void servesLibraryFixtures() {
        cli.fixture("/v7/finance/quote", "quote/aapl.json");
        HttpResponse response = send("https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL");
        assertThat(response.status()).isEqualTo(200);
        assertThat(body(response)).isEqualTo(Fixtures.load("quote/aapl.json")).contains("\"AAPL\"");
    }

    @Test
    void missingFixtureFailsLoudly() {
        assertThatThrownBy(() -> Fixtures.load("quote/nope.json"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("fixtures/quote/nope.json");
    }

    @Test
    void renderPutsExitCodeStdoutAndNonEmptyStderr() {
        assertThat(new CliResult(0, "a\n", "").render()).isEqualTo("exit: 0\n--- stdout\na\n");
        assertThat(new CliResult(3, "", "yfin: X: symbol not found\n").render())
                .isEqualTo("exit: 3\n--- stdout\n--- stderr\nyfin: X: symbol not found\n");
    }

    private HttpResponse send(String uri) {
        return cli.transport().send(HttpRequest.get(URI.create(uri)));
    }

    private static String body(HttpResponse response) {
        return new String(response.body(), StandardCharsets.UTF_8);
    }
}
