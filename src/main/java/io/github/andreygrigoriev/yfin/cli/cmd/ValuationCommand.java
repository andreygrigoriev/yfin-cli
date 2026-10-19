package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.StatementSpecs;
import io.github.andreygrigoriev.yfin.model.ValuationMeasures;
import io.github.andreygrigoriev.yfin.request.Frequency;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin valuation SYMBOL [--quarterly|--ttm]}: {@link Ticker#valuationMeasures(Frequency)}
 * as measures by period, newest first, after a {@code current} column with each measure's latest
 * trailing value. Annual by default. A symbol without valuation data prints an empty result.
 */
@Command(name = "valuation", description = "Valuation measures (market cap, P/E, EV/EBITDA, ...) by period.")
public final class ValuationCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
    String symbol = "";

    @ArgGroup(exclusive = true)
    @Nullable
    PeriodOptions period;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Ticker ticker = Commands.ticker(spec, root.client(), symbol);
        ValuationMeasures measures = ticker.valuationMeasures(PeriodOptions.frequency(period));
        root.output().list(StatementSpecs.of(measures), StatementSpecs.rows(measures));
        return 0;
    }
}
