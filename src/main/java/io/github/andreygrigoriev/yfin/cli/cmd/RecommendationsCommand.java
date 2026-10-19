package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code yfin recommendations SYMBOL [--summary]}: analyst rating counts per month, current month
 * first. {@code --summary} reads {@link Ticker#recommendationsSummary()}, which, as in yfinance, is
 * the same table.
 */
@Command(name = "recommendations", description = "Analyst rating counts per month, current month first.")
public final class RecommendationsCommand extends SymbolCommand {
    @Option(names = "--summary", description = "yfinance's recommendations_summary (the same counts).")
    boolean summary;

    @Override
    void print(Ticker ticker, Output output) {
        output.list(Specs.RECOMMENDATION, summary ? ticker.recommendationsSummary() : ticker.recommendations());
    }
}
