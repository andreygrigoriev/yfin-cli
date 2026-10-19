package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code yfin info SYMBOL}: the curated fields of {@link io.github.andreygrigoriev.yfin.model.TickerInfo}, one record. */
@Command(name = "info", description = "Profile, price, valuation and key statistics of a symbol.")
public final class InfoCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
    String symbol = "";

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Ticker ticker = Commands.ticker(spec, root.client(), symbol);
        root.output().record(Specs.TICKER_INFO, ticker.info());
        return 0;
    }
}
