package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.FastInfo;
import io.github.andreygrigoriev.yfin.Tickers;
import io.github.andreygrigoriev.yfin.cli.MultiSymbol;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code yfin quote SYMBOL...}: the {@link FastInfo} snapshot of each symbol, one row per symbol. */
@Command(name = "quote", description = "Last price, day range and year range of one or more symbols.")
public final class QuoteCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "SYMBOL", description = "Ticker symbols, e.g. AAPL BTC-USD.")
    List<String> symbols = new ArrayList<>();

    @Override
    public Integer call() throws InterruptedException {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Tickers tickers = Commands.tickers(spec, root.client(), symbols);
        MultiSymbol.Result<FastInfo> result = MultiSymbol.fetch(List.copyOf(tickers.symbols()), symbol -> {
            FastInfo info = tickers.ticker(symbol).fastInfo();
            // FastInfo is lazy: fetch every source here, so a failure belongs to this symbol
            info.toMap();
            return info;
        });
        root.output().list(Specs.FAST_INFO.withSymbol(), result.successes());
        return result.reportFailures(spec.commandLine().getErr(), root.options().verbose());
    }
}
