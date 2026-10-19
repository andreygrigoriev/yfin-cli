package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class VersionTest {
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    private int run(String... args) {
        return YfinCommand.commandLine(new YfinCommand())
                .setOut(new PrintWriter(out, true))
                .setErr(new PrintWriter(err, true))
                .execute(args);
    }

    @Test
    void versionPrintsCliAndCoreVersions() {
        assertThat(run("--version")).isZero();
        assertThat(out.toString())
                .isEqualTo("yfin " + BuildInfo.VERSION + System.lineSeparator() + "yfin-core "
                        + BuildInfo.YFIN_CORE_VERSION + System.lineSeparator());
        assertThat(BuildInfo.VERSION).doesNotContain("${");
        assertThat(BuildInfo.YFIN_CORE_VERSION).isEqualTo("0.2.1");
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void helpExitsZero() {
        assertThat(run("--help")).isZero();
        assertThat(out.toString()).contains("Usage: yfin").contains("--version");
    }

    @Test
    void unknownOptionIsUsageError() {
        assertThat(run("--bogus")).isEqualTo(2);
        assertThat(out.toString()).isEmpty();
        assertThat(err.toString()).contains("Unknown option: '--bogus'");
    }

    @Test
    void noSubcommandIsUsageError() {
        assertThat(run()).isEqualTo(2);
        assertThat(err.toString()).contains("Missing required subcommand");
    }
}
