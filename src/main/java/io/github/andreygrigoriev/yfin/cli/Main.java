package io.github.andreygrigoriev.yfin.cli;

/** Process entry point: runs the root command and exits with its code. */
public final class Main {
    private Main() {}

    /**
     * Runs {@code yfin} with the given arguments. The client of the invocation is closed before
     * the process exits.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode;
        try (YfinCommand root = new YfinCommand()) {
            exitCode = YfinCommand.commandLine(root).execute(args);
        }
        System.exit(exitCode);
    }
}
