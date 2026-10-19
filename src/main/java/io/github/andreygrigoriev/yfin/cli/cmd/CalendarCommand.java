package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.request.CalendarRequest;
import io.github.andreygrigoriev.yfin.request.EarningsCalendarRequest;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * {@code yfin calendar earnings|ipo|splits [--from DATE] [--to DATE] [--limit N]}: market-wide
 * calendars, one list per subcommand. The window defaults to today plus {@value
 * CalendarRequest#DEFAULT_DAYS} days, both ends inclusive; {@code events SYMBOL} is the calendar of
 * one ticker.
 */
@Command(
        name = "calendar",
        description = "Market-wide earnings, IPO and splits calendars.",
        subcommands = {CalendarCommand.Earnings.class, CalendarCommand.Ipo.class, CalendarCommand.Splits.class})
public final class CalendarCommand implements Runnable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Override
    public void run() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /** A calendar subcommand: the window and row limit, validated before any request. */
    abstract static class Window implements Callable<Integer> {
        @Spec
        @Nullable
        CommandSpec spec;

        @Option(names = "--from", paramLabel = "DATE", description = "First day (inclusive); default: today.")
        @Nullable
        LocalDate from;

        @Option(
                names = "--to",
                paramLabel = "DATE",
                description = "Last day (inclusive); default: " + CalendarRequest.DEFAULT_DAYS + " days after --from.")
        @Nullable
        LocalDate to;

        @Option(
                names = "--limit",
                paramLabel = "N",
                description = "Number of rows, 1.." + CalendarRequest.MAX_LIMIT + " (default: ${DEFAULT-VALUE}).")
        int limit = CalendarRequest.DEFAULT_LIMIT;

        @Override
        public Integer call() {
            CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
            if (limit < 1 || limit > CalendarRequest.MAX_LIMIT) {
                throw new ParameterException(
                        spec.commandLine(), "--limit must be in 1.." + CalendarRequest.MAX_LIMIT + ", was " + limit);
            }
            LocalDate start = from != null ? from : LocalDate.now();
            LocalDate end = to != null ? to : start.plusDays(CalendarRequest.DEFAULT_DAYS);
            if (end.isBefore(start)) {
                throw new ParameterException(spec.commandLine(), "--to " + end + " is before --from " + start);
            }
            YfinCommand root = YfinCommand.root(spec);
            print(root.client(), start, end, root.output());
            return 0;
        }

        /** Fetches the calendar of the validated window and prints it. */
        abstract void print(YFinance client, LocalDate start, LocalDate end, Output output);

        /** The window and limit as a library request. */
        CalendarRequest request(LocalDate start, LocalDate end) {
            return CalendarRequest.between(start, end).limit(limit);
        }
    }

    /** {@code calendar earnings}: {@link YFinance#earningsCalendar(EarningsCalendarRequest)}. */
    @Command(name = "earnings", description = "Earnings announcements, by default of today's most active stocks only.")
    static final class Earnings extends Window {
        @Option(names = "--all", description = "Include every company, not only today's most active stocks.")
        boolean all;

        @Option(
                names = "--market-cap",
                paramLabel = "USD",
                description = "Keep companies with at least this intraday market cap.")
        @Nullable
        Double marketCap;

        @Override
        public Integer call() {
            if (marketCap != null && (!(marketCap > 0) || marketCap.isInfinite())) {
                throw new ParameterException(
                        Objects.requireNonNull(spec, "spec is injected by picocli")
                                .commandLine(),
                        "--market-cap must be positive, was " + marketCap);
            }
            return super.call();
        }

        @Override
        void print(YFinance client, LocalDate start, LocalDate end, Output output) {
            EarningsCalendarRequest request =
                    EarningsCalendarRequest.between(start, end).limit(limit).filterMostActive(!all);
            if (marketCap != null) {
                request = request.marketCap(marketCap);
            }
            output.list(Specs.EARNINGS_EVENT, client.earningsCalendar(request));
        }
    }

    /** {@code calendar ipo}: {@link YFinance#ipoCalendar(CalendarRequest)}. */
    @Command(name = "ipo", description = "Expected and recent IPOs.")
    static final class Ipo extends Window {
        @Override
        void print(YFinance client, LocalDate start, LocalDate end, Output output) {
            output.list(Specs.IPO_EVENT, client.ipoCalendar(request(start, end)));
        }
    }

    /** {@code calendar splits}: {@link YFinance#splitsCalendar(CalendarRequest)}. */
    @Command(name = "splits", description = "Upcoming and recent stock splits.")
    static final class Splits extends Window {
        @Override
        void print(YFinance client, LocalDate start, LocalDate end, Output output) {
            output.list(Specs.SPLIT_EVENT, client.splitsCalendar(request(start, end)));
        }
    }
}
