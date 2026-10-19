package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.model.SharesCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin shares SYMBOL [--start DATE] [--end DATE]}: shares outstanding as reported, oldest
 * first, via {@link Ticker#sharesFull(LocalDate, LocalDate)}. Yahoo may report several counts per
 * day; all are printed.
 */
@Command(name = "shares", description = "Shares outstanding of a symbol over time, oldest first.")
public final class SharesCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
    String symbol = "";

    @Option(
            names = "--start",
            paramLabel = "DATE",
            description = "First day (inclusive); default: 548 days before --end.")
    @Nullable
    LocalDate start;

    @Option(names = "--end", paramLabel = "DATE", description = "Last day (exclusive); default: tomorrow.")
    @Nullable
    LocalDate end;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ParameterException(spec.commandLine(), "--start " + start + " is not before --end " + end);
        }
        YfinCommand root = YfinCommand.root(spec);
        Ticker ticker = Commands.ticker(spec, root.client(), symbol);
        List<SharesCount> shares = ticker.sharesFull(start, end);
        root.output().list(Specs.SHARES, shares);
        return 0;
    }
}
