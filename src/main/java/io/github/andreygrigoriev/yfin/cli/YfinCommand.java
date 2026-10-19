package io.github.andreygrigoriev.yfin.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Root {@code yfin} command. Subcommands hang off it; on its own it only reports usage. */
@Command(
        name = "yfin",
        mixinStandardHelpOptions = true,
        versionProvider = YfinCommand.VersionProvider.class,
        description = "Yahoo Finance market data from the command line.")
public final class YfinCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /** Prints the CLI version and the yfin-core version it was built with. */
    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"yfin " + BuildInfo.VERSION, "yfin-core " + BuildInfo.YFIN_CORE_VERSION};
        }
    }
}
