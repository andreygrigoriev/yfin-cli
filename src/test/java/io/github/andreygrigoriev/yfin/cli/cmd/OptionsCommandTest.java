package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.cli.testing.Fixtures;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import org.junit.jupiter.api.Test;

/**
 * {@code options} reads {@code /v7/finance/options}: the plain request lists the expirations and
 * carries the nearest chain (2026-10-12 in the AAPL recording), a later chain takes a second request
 * with Yahoo's epoch {@code date}. 2026-10-14 is listed but Yahoo returned no contracts for it; an
 * unknown symbol has no expirations; a malformed {@code date} is Yahoo's 400 error envelope.
 */
class OptionsCommandTest {
    private static final String OPTIONS = "/v7/finance/options/";

    private final CliRunner cli = new CliRunner();

    @Test
    void expirations() {
        cli.fixture(OPTIONS + "AAPL", "options/aapl.json");
        Golden.assertMatches(cli.run("options", "AAPL"), "options/normal.table");
        Golden.assertMatches(cli.run("options", "AAPL", "--format", "json"), "options/normal.json");
        Golden.assertMatches(cli.run("options", "AAPL", "--format", "csv"), "options/normal.csv");
    }

    @Test
    void nearestChainTakesOneRequest() {
        cli.fixture(OPTIONS + "AAPL", "options/aapl.json");
        Golden.assertMatches(cli.run("options", "AAPL", "--expiry", "2026-10-12"), "options/chain.table");
        assertThat(cli.transport().requests())
                .filteredOn(r -> r.uri().getPath().startsWith(OPTIONS))
                .hasSize(1);
        Golden.assertMatches(
                cli.run("options", "AAPL", "--expiry", "2026-10-12", "--format", "json"), "options/chain.json");
        Golden.assertMatches(
                cli.run("options", "AAPL", "--expiry", "2026-10-12", "--format", "csv"), "options/chain.csv");
    }

    @Test
    void laterChainFilteredBySide() {
        cli.onGet(OPTIONS + "AAPL")
                .query("date", "1863475200")
                .respond(200, Fixtures.load("options/aapl-2029-01-19.json"));
        cli.fixture(OPTIONS + "AAPL", "options/aapl.json");
        Golden.assertMatches(cli.run("options", "AAPL", "--expiry", "2029-01-19", "--puts"), "options/puts.table");
        Golden.assertMatches(
                cli.run("options", "AAPL", "--expiry", "2029-01-19", "--calls", "--format", "json"),
                "options/calls.json");
    }

    @Test
    void listedExpirationWithoutContractsIsEmpty() {
        cli.onGet(OPTIONS + "AAPL")
                .query("date", "1791936000")
                .respond(200, Fixtures.load("options/aapl-not-an-expiration.json"));
        cli.fixture(OPTIONS + "AAPL", "options/aapl.json");
        Golden.assertMatches(cli.run("options", "AAPL", "--expiry", "2026-10-14"), "options/empty.table");
        Golden.assertMatches(
                cli.run("options", "AAPL", "--expiry", "2026-10-14", "--format", "json"), "options/empty.json");
        Golden.assertMatches(
                cli.run("options", "AAPL", "--expiry", "2026-10-14", "--format", "csv"), "options/empty.csv");
    }

    @Test
    void unknownSymbolHasNoExpirations() {
        cli.fixture(OPTIONS + "NOSUCHSYMBOLXYZ", "options/unknown.json");
        Golden.assertMatches(cli.run("options", "NOSUCHSYMBOLXYZ"), "options/unknown.table");
        Golden.assertMatches(cli.run("options", "NOSUCHSYMBOLXYZ", "--format", "json"), "options/unknown.json");
    }

    @Test
    void unlistedExpiryIsUsageErrorListingValidDates() {
        cli.fixture(OPTIONS + "AAPL", "options/aapl.json");
        Golden.assertMatches(cli.run("options", "AAPL", "--expiry", "2026-10-13"), "options/not-an-expiration.table");
    }

    @Test
    void errorEnvelope() {
        cli.onGet(OPTIONS + "AAPL").respond(400, Fixtures.load("options/bad-date.json"));
        Golden.assertMatches(cli.run("options", "AAPL"), "options/error.table");
        Golden.assertMatches(cli.run("options", "AAPL", "--format", "json"), "options/error.json");
    }

    @Test
    void callsAndPutsAreExclusive() {
        CliResult result = cli.run("options", "AAPL", "--expiry", "2026-10-12", "--calls", "--puts");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("mutually exclusive");
        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void sideNeedsExpiry() {
        CliResult result = cli.run("options", "AAPL", "--calls");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--calls and --puts need --expiry");
        assertThat(cli.transport().requests()).isEmpty();
    }
}
