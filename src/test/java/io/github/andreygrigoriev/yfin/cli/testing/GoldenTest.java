package io.github.andreygrigoriev.yfin.cli.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenTest {
    private static final CliResult RESULT = new CliResult(3, "", "yfin: NOPE: symbol not found\n");

    @TempDir
    Path root;

    @Test
    void updateWritesTheFileCreatingDirectories() throws IOException {
        new Golden(root, true).check(RESULT, "quote/not-found.table");
        assertThat(Files.readString(root.resolve("quote/not-found.table"))).isEqualTo(RESULT.render());
    }

    @Test
    void updateOverwritesAStaleFile() throws IOException {
        Files.createDirectories(root.resolve("quote"));
        Files.writeString(root.resolve("quote/not-found.table"), "stale");
        new Golden(root, true).check(RESULT, "quote/not-found.table");
        assertThat(Files.readString(root.resolve("quote/not-found.table"))).isEqualTo(RESULT.render());
    }

    @Test
    void compareAcceptsAMatchingFile() throws IOException {
        Files.writeString(root.resolve("case.table"), RESULT.render());
        new Golden(root, false).check(RESULT, "case.table");
    }

    @Test
    void compareRejectsADifferentExitCodeOrOutput() throws IOException {
        Files.writeString(root.resolve("case.table"), RESULT.render());
        Golden golden = new Golden(root, false);
        assertThatThrownBy(() -> golden.check(new CliResult(0, "", RESULT.err()), "case.table"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("case.table");
        assertThatThrownBy(() -> golden.check(new CliResult(3, "", "yfin: other\n"), "case.table"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> golden.check(new CliResult(3, "x\n", RESULT.err()), "case.table"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void compareFailsOnAMissingFileWithAHint() {
        assertThatThrownBy(() -> new Golden(root, false).check(RESULT, "quote/none.json"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("golden file missing")
                .hasMessageContaining("-Dgolden.update=true");
        assertThat(root.resolve("quote")).doesNotExist();
    }
}
