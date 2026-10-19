package io.github.andreygrigoriev.yfin.cli.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import io.github.andreygrigoriev.yfin.cli.testing.CliResult;
import io.github.andreygrigoriev.yfin.cli.testing.CliRunner;
import io.github.andreygrigoriev.yfin.spi.Cache;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompletionCommandTest {
    private final CliRunner cli = new CliRunner();

    @Test
    void printsBashScriptWithEveryTopLevelCommand() {
        CliResult result = cli.run("completion");

        assertThat(result.exitCode()).isZero();
        assertThat(result.err()).isEmpty();
        assertThat(result.out())
                .startsWith("#!/usr/bin/env bash")
                .contains("complete -F _complete_yfin")
                .contains("_picocli_yfin_financials_income")
                .contains("_picocli_yfin_calendar_earnings");
        List<String> commands = List.copyOf(YfinCommand.commandLine(new YfinCommand(
                        options -> YFinance.builder().cache(Cache.none()).build(), Terminal.piped(120)))
                .getSubcommands()
                .keySet());
        assertThat(commands).contains("quote", "calendar", "completion");
        // the root function lists the top-level commands in declaration order
        assertThat(result.out()).contains("local commands=\"" + String.join(" ", commands) + "\"\n");
    }

    @Test
    void makesNoRequests() {
        cli.run("completion");

        assertThat(cli.transport().requests()).isEmpty();
    }

    @Test
    void rejectsArguments() {
        CliResult result = cli.run("completion", "zsh");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.out()).isEmpty();
    }
}
