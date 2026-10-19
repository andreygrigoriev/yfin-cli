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

/** {@code yfin dividends SYMBOL}: every {@link io.github.andreygrigoriev.yfin.model.Dividend} of the symbol's full daily history. */
@Command(name = "dividends", description = "All dividends of a symbol, oldest first.")
public final class DividendsCommand implements Callable<Integer> {
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
        root.output().list(Specs.DIVIDEND, ticker.dividends());
        return 0;
    }
}
