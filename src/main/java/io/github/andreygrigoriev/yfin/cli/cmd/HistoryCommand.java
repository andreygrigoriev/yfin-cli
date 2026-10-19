package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Tickers;
import io.github.andreygrigoriev.yfin.cli.ErrorHandler;
import io.github.andreygrigoriev.yfin.cli.ExitCodes;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.exception.YFDownloadException;
import io.github.andreygrigoriev.yfin.exception.YFinanceException;
import io.github.andreygrigoriev.yfin.model.Bar;
import io.github.andreygrigoriev.yfin.model.PriceHistory;
import io.github.andreygrigoriev.yfin.request.HistoryRequest;
import io.github.andreygrigoriev.yfin.request.Interval;
import io.github.andreygrigoriev.yfin.request.Period;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * {@code yfin history SYMBOL...}: price bars via {@link Tickers#history(HistoryRequest)}. With one
 * symbol the rows are its bars; with several, each row starts with its symbol.
 */
@Command(name = "history", description = "Price bars (OHLC, adjusted close, volume) of one or more symbols.")
public final class HistoryCommand implements Callable<Integer> {
    @picocli.CommandLine.Spec
    @Nullable
    CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "SYMBOL", description = "Ticker symbols, e.g. AAPL MSFT.")
    List<String> symbols = new ArrayList<>();

    @Option(
            names = "--period",
            paramLabel = "PERIOD",
            description = "Range up to now: 1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max (default: 1mo).")
    @Nullable
    Period period;

    @Option(names = "--start", paramLabel = "DATE", description = "First day (inclusive), e.g. 2026-01-02.")
    @Nullable
    LocalDate start;

    @Option(names = "--end", paramLabel = "DATE", description = "Last day (exclusive); default: today.")
    @Nullable
    LocalDate end;

    @Option(
            names = "--interval",
            paramLabel = "INTERVAL",
            description = "Bar size: 1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo (default: 1d).")
    Interval interval = Interval.ONE_DAY;

    @Option(names = "--prepost", description = "Include pre- and post-market bars (intraday intervals).")
    boolean prepost;

    @Option(names = "--no-adjust", description = "Raw prices instead of dividend- and split-adjusted ones.")
    boolean noAdjust;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        HistoryRequest request = request(spec);
        Tickers tickers = Commands.tickers(spec, root.client(), symbols);

        Map<String, PriceHistory> histories;
        Map<String, YFinanceException> failures = Map.of();
        try {
            histories = tickers.history(request);
        } catch (YFDownloadException e) {
            histories = e.partialResults();
            failures = e.failures();
        }

        var bars = Specs.bars(request.interval());
        if (tickers.symbols().size() == 1) {
            List<Bar> rows =
                    histories.values().stream().flatMap(h -> h.bars().stream()).toList();
            root.output().list(bars, rows);
        } else {
            List<Symbol<Bar>> rows = new ArrayList<>();
            histories.forEach((symbol, history) -> history.bars().forEach(bar -> rows.add(new Symbol<>(symbol, bar))));
            root.output().list(bars.withSymbol(), rows);
        }

        PrintWriter err = spec.commandLine().getErr();
        int exitCode = ExitCodes.OK;
        for (Map.Entry<String, YFinanceException> failure : failures.entrySet()) {
            int code = ErrorHandler.report(
                    err, failure.getKey(), failure.getValue(), root.options().verbose());
            if (exitCode == ExitCodes.OK) {
                exitCode = code;
            }
        }
        return exitCode;
    }

    private HistoryRequest request(CommandSpec spec) {
        if (period != null && (start != null || end != null)) {
            throw new ParameterException(spec.commandLine(), "--period cannot be combined with --start or --end");
        }
        HistoryRequest.Builder builder =
                HistoryRequest.builder().interval(interval).prepost(prepost).autoAdjust(!noAdjust);
        if (period != null) {
            builder.period(period);
        }
        if (start != null) {
            builder.start(start);
        }
        if (end != null) {
            builder.end(end);
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage());
        }
    }
}
