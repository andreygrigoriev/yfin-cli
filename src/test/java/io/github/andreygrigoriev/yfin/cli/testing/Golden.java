package io.github.andreygrigoriev.yfin.cli.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares a {@link CliResult} with a golden file under {@code src/test/resources/golden}, named
 * {@code <command path>/<case>.<format>} (e.g. {@code quote/normal.table}). The file holds {@link
 * CliResult#render()}: exit code, stdout and stderr.
 *
 * <p>Run with {@code -Dgolden.update=true} to write the files instead of comparing; review the diff
 * before committing.
 */
public final class Golden {
    /** Golden files of this repo, relative to the module directory Maven runs tests from. */
    public static final Path ROOT = Path.of("src", "test", "resources", "golden");

    private static final Golden DEFAULT = new Golden(ROOT, Boolean.getBoolean("golden.update"));

    private final Path root;
    private final boolean update;

    /**
     * A golden store.
     *
     * @param root directory holding the golden files
     * @param update whether to write files instead of comparing
     */
    public Golden(Path root, boolean update) {
        this.root = root;
        this.update = update;
    }

    /**
     * Asserts that {@code result} matches the golden file {@code name} of this repo.
     *
     * @param result the CLI result
     * @param name golden file path below the golden root
     */
    public static void assertMatches(CliResult result, String name) {
        DEFAULT.check(result, name);
    }

    /**
     * Compares {@code result} with the golden file {@code name}, or writes it in update mode.
     *
     * @param result the CLI result
     * @param name golden file path below the golden root
     */
    public void check(CliResult result, String name) {
        Path file = root.resolve(name);
        String actual = result.render();
        try {
            if (update) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, actual, StandardCharsets.UTF_8);
                return;
            }
            if (!Files.isRegularFile(file)) {
                throw new AssertionError(
                        "golden file missing: " + file + " (run with -Dgolden.update=true to create it)");
            }
            String expected = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(actual).as("golden file %s", file).isEqualTo(expected);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot access golden file " + file, e);
        }
    }
}
