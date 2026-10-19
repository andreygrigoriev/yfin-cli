package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import picocli.CommandLine.Command;

/**
 * {@code yfin price-target SYMBOL}: analyst price targets next to the current price ({@link
 * Ticker#analystPriceTargets()}); an empty record when Yahoo has none, e.g. for an ETF.
 */
@Command(name = "price-target", description = "Analyst price targets: low, mean, median and high.")
public final class PriceTargetCommand extends SymbolCommand {
    @Override
    void print(Ticker ticker, Output output) {
        output.record(Specs.PRICE_TARGETS, ticker.analystPriceTargets().orElse(null));
    }
}
