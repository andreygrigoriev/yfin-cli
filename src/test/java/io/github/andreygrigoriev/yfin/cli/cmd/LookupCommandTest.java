package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@code lookup} reads {@code /v1/finance/lookup}; {@code --type} becomes Yahoo's {@code type}. */
class LookupCommandTest {
    private static final String LOOKUP = "/v1/finance/lookup";

    private final CliRunner cli = new CliRunner();

    @Test
    void all() {
        cli.onGet(LOOKUP).query("type", "all").respond(200, Fixtures.load("lookup/aapl-all.json"));
        Golden.assertMatches(cli.run("lookup", "aapl"), "lookup/normal.table");
        Golden.assertMatches(cli.run("lookup", "aapl", "--format", "json"), "lookup/normal.json");
        Golden.assertMatches(cli.run("lookup", "aapl", "--format", "csv"), "lookup/normal.csv");
    }

    @Test
    void byType() {
        cli.onGet(LOOKUP).query("type", "etf").respond(200, Fixtures.load("lookup/gold-etf.json"));
        cli.onGet(LOOKUP).query("type", "cryptocurrency").respond(200, Fixtures.load("lookup/btc-crypto.json"));
        Golden.assertMatches(cli.run("lookup", "gold", "--type", "etf"), "lookup/etf.table");
        Golden.assertMatches(
                cli.run("lookup", "btc", "--type", "cryptocurrency", "--format", "json"), "lookup/crypto.json");
    }

    @Test
    void countReachesTheRequest() {
        cli.fixture(LOOKUP, "lookup/empty.json");
        assertThat(cli.run("lookup", "aapl", "--count", "5").exitCode()).isZero();
        assertThat(cli.transport().requests().stream()
                        .filter(r -> r.uri().getPath().equals(LOOKUP))
                        .findFirst()
                        .orElseThrow()
                        .uri()
                        .getRawQuery())
                .contains("count=5");
    }

    @Test
    void empty() {
        cli.fixture(LOOKUP, "lookup/empty.json");
        Golden.assertMatches(cli.run("lookup", "zzzzqqq"), "lookup/empty.table");
        Golden.assertMatches(cli.run("lookup", "zzzzqqq", "--format", "json"), "lookup/empty.json");
        Golden.assertMatches(cli.run("lookup", "zzzzqqq", "--format", "csv"), "lookup/empty.csv");
    }

    @Test
    void badType() {
        cli.onGet(LOOKUP).respond(400, Fixtures.load("lookup/bad-type.json"));
        Golden.assertMatches(cli.run("lookup", "aapl"), "lookup/bad-type.table");
        Golden.assertMatches(cli.run("lookup", "aapl", "--format", "json"), "lookup/bad-type.json");
    }

    @Test
    void usageErrors() {
        for (String[] args : List.of(
                new String[] {"lookup", "aapl", "--type", "bond"},
                new String[] {"lookup", "aapl", "--count", "0"},
                new String[] {"lookup", " "})) {
            CliResult result = cli.run(args);
            assertThat(result.exitCode()).as(String.join(" ", args)).isEqualTo(2);
        }
        assertThat(cli.transport().requests()).isEmpty();
    }
}
