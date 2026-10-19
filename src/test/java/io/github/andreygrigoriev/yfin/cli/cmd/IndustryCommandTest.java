package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/** {@code industry} reads {@code /v1/finance/industries/<key>}: the overview and three company lists. */
class IndustryCommandTest {
    private static final String INDUSTRIES = "/v1/finance/industries/";

    private final CliRunner cli = new CliRunner();

    @Test
    void overview() {
        cli.fixture(INDUSTRIES + "semiconductors", "industry/semiconductors.json");
        Golden.assertMatches(cli.run("industry", "semiconductors"), "industry/normal.table");
        Golden.assertMatches(cli.run("industry", "semiconductors", "--format", "json"), "industry/normal.json");
        Golden.assertMatches(cli.run("industry", "semiconductors", "--format", "csv"), "industry/normal.csv");
    }

    @Test
    void companyLists() {
        cli.fixture(INDUSTRIES + "semiconductors", "industry/semiconductors.json");
        Golden.assertMatches(cli.run("industry", "semiconductors", "--top"), "industry/top.table");
        Golden.assertMatches(cli.run("industry", "semiconductors", "--performing"), "industry/performing.table");
        Golden.assertMatches(
                cli.run("industry", "semiconductors", "--performing", "--format", "json"), "industry/performing.json");
        Golden.assertMatches(cli.run("industry", "semiconductors", "--growth"), "industry/growth.table");
        Golden.assertMatches(
                cli.run("industry", "semiconductors", "--growth", "--format", "csv"), "industry/growth.csv");
    }

    @Test
    void empty() {
        cli.fixture(INDUSTRIES + "solar", "industry/minimal.json");
        Golden.assertMatches(cli.run("industry", "solar"), "industry/empty.table");
        Golden.assertMatches(cli.run("industry", "solar", "--format", "json"), "industry/empty.json");
        Golden.assertMatches(cli.run("industry", "solar", "--top", "--format", "json"), "industry/empty-top.json");
    }

    @Test
    void unknown() {
        cli.onGet(INDUSTRIES + "notanindustry").respond(404, Fixtures.load("industry/unknown.html"));
        Golden.assertMatches(cli.run("industry", "notanindustry"), "industry/unknown.table");
        Golden.assertMatches(cli.run("industry", "notanindustry", "--format", "json"), "industry/unknown.json");
    }

    @Test
    void error() {
        // the library's error payload of the sector endpoint, which the industry endpoint shares
        cli.onGet(INDUSTRIES + "semiconductors").respond(400, Fixtures.load("sector/error.json"));
        Golden.assertMatches(cli.run("industry", "semiconductors", "--top"), "industry/error.table");
    }

    @Test
    void usageErrors() {
        assertThat(cli.run("industry", " ").exitCode()).isEqualTo(2);
        assertThat(cli.run("industry", "solar", "--top", "--growth").exitCode()).isEqualTo(2);
        assertThat(cli.transport().requests()).isEmpty();
    }
}
