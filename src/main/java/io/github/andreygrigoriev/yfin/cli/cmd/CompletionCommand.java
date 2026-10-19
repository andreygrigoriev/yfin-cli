package io.github.andreygrigoriev.yfin.cli.cmd;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * {@code yfin completion}: prints a bash completion script for the whole command tree ({@link
 * AutoComplete#bash(String, picocli.CommandLine)}). zsh loads it too, after {@code autoload -U
 * bashcompinit && bashcompinit}.
 */
@Command(
        name = "completion",
        description = {
            "Print a bash completion script for yfin.",
            "bash: source <(yfin completion)",
            "zsh:  autoload -U bashcompinit && bashcompinit && source <(yfin completion)"
        })
public final class CompletionCommand implements Runnable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Override
    public void run() {
        CommandSpec command = Objects.requireNonNull(spec, "spec is injected by picocli");
        command.commandLine()
                .getOut()
                .print(AutoComplete.bash("yfin", command.root().commandLine()));
        command.commandLine().getOut().flush();
    }
}
