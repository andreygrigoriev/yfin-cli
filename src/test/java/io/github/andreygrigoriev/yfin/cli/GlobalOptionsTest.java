package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.output.Format;
import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.output.Spec;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import io.github.andreygrigoriev.yfin.spi.Cache;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

class GlobalOptionsTest {
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final List<GlobalOptions> created = new ArrayList<>();
    private final YfinCommand root = new YfinCommand(
            options -> {
                created.add(options);
                return YFinance.builder().cache(Cache.none()).build();
            },
            Terminal.piped(120));
    private final Leaf leaf = new Leaf();

    private int run(String... args) {
        CommandLine commandLine = new CommandLine(root);
        commandLine.addSubcommand(new CommandLine(new Family()).addSubcommand(leaf));
        return Converters.register(commandLine)
                .setOut(new PrintWriter(out, true))
                .setErr(new PrintWriter(err, true))
                .execute(args);
    }

    @Test
    void defaults() {
        assertThat(run("family", "leaf")).isZero();
        GlobalOptions options = leaf.seen();
        assertThat(options.format()).isEqualTo(Format.TABLE);
        assertThat(options.noColor()).isFalse();
        assertThat(options.noCache()).isFalse();
        assertThat(options.timeout()).isNull();
        assertThat(options.retries()).isNull();
        assertThat(options.verbose()).isFalse();
    }

    @Test
    void optionsAfterTheLeafReachTheRoot() {
        assertThat(run(
                        "family",
                        "leaf",
                        "--format",
                        "csv",
                        "--no-color",
                        "--no-cache",
                        "--timeout",
                        "5s",
                        "--retries",
                        "0",
                        "--verbose"))
                .isZero();
        GlobalOptions options = leaf.seen();
        assertThat(options).isSameAs(root.options());
        assertThat(options.format()).isEqualTo(Format.CSV);
        assertThat(options.noColor()).isTrue();
        assertThat(options.noCache()).isTrue();
        assertThat(options.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(options.retries()).isZero();
        assertThat(options.verbose()).isTrue();
    }

    @Test
    void optionsBeforeTheSubcommandAndOnTheFamilyAlsoApply() {
        assertThat(run("--format", "json", "family", "--retries", "3", "leaf")).isZero();
        assertThat(leaf.seen().format()).isEqualTo(Format.JSON);
        assertThat(leaf.seen().retries()).isEqualTo(3);
    }

    @Test
    void outputUsesTheChosenFormatAndTheCommandLineWriters() {
        assertThat(run("family", "leaf", "--format", "json")).isZero();
        assertThat(out.toString()).isEqualTo("{\n  \"name\": \"leaf\"\n}\n");
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void badFormatIsUsageError() {
        assertThat(run("family", "leaf", "--format", "xml")).isEqualTo(2);
        assertThat(err.toString())
                .contains("Invalid value for option '--format'")
                .contains("'xml' is not a valid format; valid values: table, csv, json");
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void negativeRetriesIsUsageError() {
        assertThat(run("family", "leaf", "--retries", "-1")).isEqualTo(2);
        assertThat(err.toString()).contains("'-1' is not a valid count");
    }

    @Test
    void badTimeoutIsUsageError() {
        assertThat(run("--timeout", "0s", "family", "leaf")).isEqualTo(2);
        assertThat(err.toString()).contains("'0s' is not a valid duration");
    }

    @Test
    void helpListsGlobalOptionsOnSubcommands() {
        CommandLine commandLine = new CommandLine(root);
        commandLine.addSubcommand(new CommandLine(new Family()).addSubcommand(leaf));
        String usage = commandLine
                .getSubcommands()
                .get("family")
                .getSubcommands()
                .get("leaf")
                .getUsageMessage();
        assertThat(usage)
                .contains(
                        "--format=FORMAT",
                        "--no-color",
                        "--no-cache",
                        "--timeout=DURATION",
                        "--retries=N",
                        "--verbose");
    }

    @Test
    void oneClientPerInvocationClosedByTheRoot() {
        assertThat(run("family", "leaf", "--no-cache")).isZero();
        YFinance first = root.client();
        assertThat(root.client()).isSameAs(first);
        assertThat(created).hasSize(1);
        assertThat(created.getFirst().noCache()).isTrue();

        root.close();
        root.close();
        assertThat(root.client()).isNotSameAs(first);
        assertThat(created).hasSize(2);
        root.close();
    }

    @Test
    void closeWithoutClientCreatesNone() {
        assertThat(run("family", "leaf")).isZero();
        root.close();
        assertThat(created).isEmpty();
    }

    @Test
    void defaultFactoryBuildsAClientFromTheOptions() {
        assertThat(run("family", "leaf", "--no-cache", "--timeout", "2s", "--retries", "1"))
                .isZero();
        try (YFinance client = ClientFactory.DEFAULT.create(leaf.seen())) {
            assertThat(client).isNotNull();
        }
    }

    @Command(name = "family")
    static final class Family {}

    @Command(name = "leaf")
    static final class Leaf implements Runnable {
        @CommandLine.Spec
        @Nullable
        CommandSpec spec;

        private @Nullable GlobalOptions seen;

        @Override
        public void run() {
            YfinCommand root = YfinCommand.root(Objects.requireNonNull(spec));
            seen = root.options();
            Output output = root.output();
            output.record(Spec.<String>builder().col("name", s -> s, Kind.TEXT).build(), "leaf");
        }

        GlobalOptions seen() {
            assertThat(seen).isNotNull();
            return seen;
        }
    }
}
