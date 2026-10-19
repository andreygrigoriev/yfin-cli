package io.github.andreygrigoriev.yfin.cli.output;

import java.io.Console;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What the CLI knows about where stdout goes. It only changes table styling (colour, truncation),
 * never the format.
 *
 * @param tty whether stdout is an interactive terminal
 * @param width terminal width in columns, from {@code $COLUMNS} (default {@value #DEFAULT_WIDTH})
 * @param noColorEnv whether {@code NO_COLOR} is set to a non-empty value
 */
public record Terminal(boolean tty, int width, boolean noColorEnv) {
    /** Width used when {@code $COLUMNS} is unset or invalid. */
    public static final int DEFAULT_WIDTH = 120;

    /** Validates the width. */
    public Terminal {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive, got " + width);
        }
    }

    /**
     * Detects the terminal of the current process.
     *
     * @return the terminal
     */
    public static Terminal detect() {
        Console console = System.console();
        return of(console != null && console.isTerminal(), System.getenv());
    }

    /**
     * Builds a terminal from a TTY flag and environment variables.
     *
     * @param tty whether stdout is an interactive terminal
     * @param env environment variables ({@code COLUMNS}, {@code NO_COLOR})
     * @return the terminal
     */
    public static Terminal of(boolean tty, Map<String, String> env) {
        @Nullable String noColor = env.get("NO_COLOR");
        return new Terminal(tty, width(env.get("COLUMNS")), noColor != null && !noColor.isEmpty());
    }

    /**
     * A non-interactive terminal of the given width, as when output is piped.
     *
     * @param width width in columns
     * @return the terminal
     */
    public static Terminal piped(int width) {
        return new Terminal(false, width, false);
    }

    private static int width(@Nullable String columns) {
        if (columns == null) {
            return DEFAULT_WIDTH;
        }
        try {
            int width = Integer.parseInt(columns.strip());
            return width > 0 ? width : DEFAULT_WIDTH;
        } catch (NumberFormatException e) {
            return DEFAULT_WIDTH;
        }
    }
}
