package io.github.andreygrigoriev.yfin.cli.output;

/**
 * Table styling derived from the {@link Terminal}: colour and truncation are only on for a TTY.
 *
 * @param color whether to emit ANSI colour
 * @param truncate whether to shorten TEXT cells so rows fit {@code width}
 * @param width the row width to fit when truncating
 */
record Style(boolean color, boolean truncate, int width) {
    /**
     * Derives the style for a terminal.
     *
     * @param terminal the terminal
     * @param noColor whether {@code --no-color} was given
     * @return colour on a TTY unless {@code NO_COLOR} or {@code --no-color}; truncation on a TTY
     */
    static Style of(Terminal terminal, boolean noColor) {
        return new Style(terminal.tty() && !terminal.noColorEnv() && !noColor, terminal.tty(), terminal.width());
    }
}
