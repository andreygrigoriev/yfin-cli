package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code yfin isin SYMBOL}: the symbol's ISIN, one record; none found prints an empty record. */
@Command(name = "isin", description = "The ISIN of a symbol, looked up by name; empty when there is none.")
public final class IsinCommand implements Callable<Integer> {
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
        root.output()
                .record(
                        Specs.ISIN,
                        ticker.isin()
                                .map(isin -> new Symbol<>(ticker.symbol(), isin))
                                .orElse(null));
        return 0;
    }
}
