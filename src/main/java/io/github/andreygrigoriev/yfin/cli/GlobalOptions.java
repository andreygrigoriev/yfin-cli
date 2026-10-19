package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.cli.output.Format;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Options of the root command that every subcommand inherits. picocli binds an inherited option to
 * this one instance, wherever on the command line it appears.
 */
public final class GlobalOptions {
    @Option(
            names = "--format",
            scope = ScopeType.INHERIT,
            paramLabel = "FORMAT",
            description = "Output format: table, csv or json (default: table).")
    Format format = Format.TABLE;

    @Option(names = "--no-color", scope = ScopeType.INHERIT, description = "Never colour table output.")
    boolean noColor;

    @Option(
            names = "--no-cache",
            scope = ScopeType.INHERIT,
            description = "Don't read or write the on-disk cache (cookie, crumb, timezones).")
    boolean noCache;

    @Option(
            names = "--timeout",
            scope = ScopeType.INHERIT,
            paramLabel = "DURATION",
            description = "Timeout per request, e.g. 30s or 2m (default: 30s).")
    @Nullable
    Duration timeout;

    @Option(
            names = "--retries",
            scope = ScopeType.INHERIT,
            paramLabel = "N",
            converter = Converters.NonNegativeInt.class,
            description = "Retries on rate limits and server errors; 0 disables (default: 2).")
    @Nullable
    Integer retries;

    @Option(
            names = "--verbose",
            scope = ScopeType.INHERIT,
            description = "Print the cause chain of errors and stack traces of unexpected ones.")
    boolean verbose;

    /** The output format. */
    public Format format() {
        return format;
    }

    /** Whether {@code --no-color} was given. */
    public boolean noColor() {
        return noColor;
    }

    /** Whether {@code --no-cache} was given. */
    public boolean noCache() {
        return noCache;
    }

    /** The request timeout, {@code null} for the library default. */
    public @Nullable Duration timeout() {
        return timeout;
    }

    /** The retry count, {@code null} for the library default. */
    public @Nullable Integer retries() {
        return retries;
    }

    /** Whether {@code --verbose} was given. */
    public boolean verbose() {
        return verbose;
    }
}
