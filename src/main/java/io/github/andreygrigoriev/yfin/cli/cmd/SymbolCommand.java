package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * A command over one symbol that prints one library call: the symbol parameter, the client and the
 * output. Subclasses fetch and print in {@link #print(Ticker, Output)}.
 */
abstract class SymbolCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
    String symbol = "";

    /**
     * Fetches and prints the data.
     *
     * @param ticker the symbol's handle
     * @param output where to print
     */
    abstract void print(Ticker ticker, Output output);

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        print(Commands.ticker(spec, root.client(), symbol), root.output());
        return 0;
    }
}
