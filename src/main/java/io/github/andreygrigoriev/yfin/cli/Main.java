package io.github.andreygrigoriev.yfin.cli;

import picocli.CommandLine;

/** Process entry point: runs the root command and exits with its code. */
public final class Main {
    private Main() {}

    /**
     * Runs {@code yfin} with the given arguments.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new YfinCommand()).execute(args));
    }
}
