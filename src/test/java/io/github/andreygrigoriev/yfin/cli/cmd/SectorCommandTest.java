package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code sector} reads {@code /v1/finance/sectors/<key>}: the overview, industries and top companies. */
class SectorCommandTest {
    private static final String SECTORS = "/v1/finance/sectors/";

    private final CliRunner cli = new CliRunner();

    @Test
    void overview() {
        cli.fixture(SECTORS + "technology", "sector/technology.json");
        Golden.assertMatches(cli.run("sector", "Technology"), "sector/normal.table");
        Golden.assertMatches(cli.run("sector", "technology", "--format", "json"), "sector/normal.json");
        Golden.assertMatches(cli.run("sector", "technology", "--format", "csv"), "sector/normal.csv");
    }

    @Test
    void industries() {
        cli.fixture(SECTORS + "technology", "sector/technology.json");
        Golden.assertMatches(cli.run("sector", "technology", "--industries"), "sector/industries.table");
        Golden.assertMatches(
                cli.run("sector", "technology", "--industries", "--format", "json"), "sector/industries.json");
    }

    @Test
    void top() {
        cli.fixture(SECTORS + "technology", "sector/technology.json");
        Golden.assertMatches(cli.run("sector", "technology", "--top"), "sector/top.table");
        Golden.assertMatches(cli.run("sector", "technology", "--top", "--format", "csv"), "sector/top.csv");
    }

    @Test
    void empty() {
        cli.fixture(SECTORS + "utilities", "sector/minimal.json");
        Golden.assertMatches(cli.run("sector", "utilities"), "sector/empty.table");
        Golden.assertMatches(cli.run("sector", "utilities", "--format", "json"), "sector/empty.json");
        Golden.assertMatches(
                cli.run("sector", "utilities", "--industries", "--format", "json"), "sector/empty-industries.json");
        Golden.assertMatches(cli.run("sector", "utilities", "--top"), "sector/empty-top.table");
    }

    @Test
    void unknown() {
        cli.onGet(SECTORS + "notasector").respond(404, Fixtures.load("sector/unknown.html"));
        Golden.assertMatches(cli.run("sector", "notasector"), "sector/unknown.table");
        Golden.assertMatches(cli.run("sector", "notasector", "--format", "json"), "sector/unknown.json");
    }

    @Test
    void error() {
        cli.onGet(SECTORS + "technology").respond(400, Fixtures.load("sector/error.json"));
        Golden.assertMatches(cli.run("sector", "technology"), "sector/error.table");
    }

    @Test
    void usageErrors() {
        assertThat(cli.run("sector", " ").exitCode()).isEqualTo(2);
        assertThat(cli.run("sector", "technology", "--top", "--industries").exitCode())
                .isEqualTo(2);
        assertThat(cli.transport().requests()).isEmpty();
    }
}
