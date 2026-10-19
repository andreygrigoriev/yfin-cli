package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.Objects;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * {@code yfin earnings SYMBOL [--limit N]}: past and upcoming earnings announcements, newest first
 * ({@link Ticker#earningsDates(int)}). Values Yahoo doesn't have yet, such as the reported EPS of an
 * upcoming date, are missing.
 */
@Command(name = "earnings", description = "Earnings dates with EPS estimate, reported EPS and surprise.")
public final class EarningsCommand extends SymbolCommand {
    /** Yahoo's cap on the number of announcements per request. */
    static final int MAX_LIMIT = 100;

    @Option(
            names = "--limit",
            paramLabel = "N",
            description = "Number of announcements, 1.." + MAX_LIMIT + " (default: ${DEFAULT-VALUE}).")
    int limit = 12;

    @Override
    public Integer call() {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ParameterException(
                    Objects.requireNonNull(spec, "spec is injected by picocli").commandLine(),
                    "--limit must be in 1.." + MAX_LIMIT + ", was " + limit);
        }
        return super.call();
    }

    @Override
    void print(Ticker ticker, Output output) {
        output.list(Specs.EARNINGS_DATE, ticker.earningsDates(limit));
    }
}
