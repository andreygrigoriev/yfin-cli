package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import io.github.andreygrigoriev.yfin.cli.testing.Golden;
import io.github.andreygrigoriev.yfin.spi.Cache;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Every leaf command must have golden files: {@code golden/<path>/} with a {@code normal.*} case that
 * exits 0 and at least one case that exits non-zero.
 */
class GoldenCoverageTest {
    /** Leaves with no Yahoo data to golden-test. */
    private static final Set<String> EXEMPT = Set.of("help", "completion");

    @Test
    void everyLeafCommandHasGoldenFiles() {
        YfinCommand root = new YfinCommand(
                options -> YFinance.builder().cache(Cache.none()).build(), Terminal.piped(120));
        assertThat(missing(YfinCommand.commandLine(root), Golden.ROOT)).isEmpty();
    }

    @Test
    void reportsLeavesWithoutGoldenDirectory(@TempDir Path golden) throws IOException {
        write(golden.resolve("quote/normal.table"), 0);
        write(golden.resolve("quote/not-found.table"), 3);

        assertThat(missing(sampleTree(), golden))
                .containsExactly(
                        "family/a: no golden directory",
                        "family/b: no golden directory",
                        "history: no golden directory");
    }

    @Test
    void reportsLeavesWithoutNormalOrErrorCase(@TempDir Path golden) throws IOException {
        write(golden.resolve("quote/normal.table"), 0);
        write(golden.resolve("history/normal.json"), 0);
        write(golden.resolve("family/a/empty.table"), 0);
        write(golden.resolve("family/a/error.table"), 7);
        write(golden.resolve("family/b/normal.table"), 0);
        write(golden.resolve("family/b/normal.json"), 0);
        write(golden.resolve("family/b/not-found.json"), 3);

        assertThat(missing(sampleTree(), golden))
                .containsExactly(
                        "family/a: no normal.* case exiting 0",
                        "history: no case exiting non-zero",
                        "quote: no case exiting non-zero");
    }

    @Test
    void completeTreePasses(@TempDir Path golden) throws IOException {
        for (String leaf : List.of("quote", "history", "family/a", "family/b")) {
            write(golden.resolve(leaf + "/normal.table"), 0);
            write(golden.resolve(leaf + "/error.table"), 6);
        }
        assertThat(missing(sampleTree(), golden)).isEmpty();
    }

    /** Problems with the golden files of each leaf under {@code commandLine}, sorted by path. */
    static List<String> missing(CommandLine commandLine, Path golden) {
        List<String> leaves = new ArrayList<>();
        collectLeaves(commandLine, "", leaves);
        List<String> problems = new ArrayList<>();
        for (String leaf : leaves.stream().sorted().toList()) {
            Path dir = golden.resolve(leaf);
            if (!Files.isDirectory(dir)) {
                problems.add(leaf + ": no golden directory");
                continue;
            }
            List<Path> files = list(dir);
            boolean normal =
                    files.stream().anyMatch(f -> f.getFileName().toString().startsWith("normal.") && exitCode(f) == 0);
            boolean error = files.stream().anyMatch(f -> exitCode(f) != 0);
            if (!normal) {
                problems.add(leaf + ": no normal.* case exiting 0");
            }
            if (!error) {
                problems.add(leaf + ": no case exiting non-zero");
            }
        }
        return problems;
    }

    private static void collectLeaves(CommandLine command, String path, List<String> leaves) {
        command.getSubcommands().forEach((name, sub) -> {
            // aliases map to the same CommandLine; only the primary name counts
            if (!name.equals(sub.getCommandName()) || EXEMPT.contains(name)) {
                return;
            }
            String subPath = path.isEmpty() ? name : path + "/" + name;
            if (sub.getSubcommands().isEmpty()) {
                leaves.add(subPath);
            } else {
                collectLeaves(sub, subPath, leaves);
            }
        });
    }

    private static List<Path> list(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The exit code on the first line ({@code exit: N}) of a golden file, or -1 if there is none. */
    private static int exitCode(Path file) {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.findFirst()
                    .filter(line -> line.startsWith("exit: "))
                    .map(line -> Integer.parseInt(line.substring("exit: ".length())))
                    .orElse(-1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, int exitCode) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "exit: " + exitCode + "\n--- stdout\n");
    }

    private static CommandLine sampleTree() {
        return new CommandLine(CommandSpec.create())
                .addSubcommand("quote", CommandSpec.create())
                .addSubcommand("history", CommandSpec.create())
                .addSubcommand(
                        "family",
                        new CommandLine(CommandSpec.create())
                                .addSubcommand("a", CommandSpec.create())
                                .addSubcommand("b", CommandSpec.create())
                                .addSubcommand("help", new CommandLine.HelpCommand()))
                .addSubcommand("completion", CommandSpec.create())
                .addSubcommand("help", new CommandLine.HelpCommand());
    }
}
