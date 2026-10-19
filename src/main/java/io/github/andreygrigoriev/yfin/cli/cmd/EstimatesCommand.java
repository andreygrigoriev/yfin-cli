package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * {@code yfin estimates earnings|revenue|history|eps-trend|eps-revisions|growth SYMBOL}: analyst
 * estimates, one list per subcommand. A symbol without analyst coverage (an ETF, a crypto pair)
 * prints an empty list.
 */
@Command(
        name = "estimates",
        description = "Analyst estimates: EPS, revenue, EPS history, trend, revisions and growth.",
        subcommands = {
            EstimatesCommand.Earnings.class,
            EstimatesCommand.Revenue.class,
            EstimatesCommand.History.class,
            EstimatesCommand.EpsTrend.class,
            EstimatesCommand.EpsRevisions.class,
            EstimatesCommand.Growth.class
        })
public final class EstimatesCommand implements Runnable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Override
    public void run() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /** {@code estimates earnings}: {@link Ticker#earningsEstimate()}. */
    @Command(name = "earnings", description = "EPS estimates for the current and next quarter and year.")
    static final class Earnings extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.EARNINGS_ESTIMATE, ticker.earningsEstimate());
        }
    }

    /** {@code estimates revenue}: {@link Ticker#revenueEstimate()}. */
    @Command(name = "revenue", description = "Revenue estimates for the current and next quarter and year.")
    static final class Revenue extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.REVENUE_ESTIMATE, ticker.revenueEstimate());
        }
    }

    /** {@code estimates history}: {@link Ticker#earningsHistory()}. */
    @Command(name = "history", description = "Reported versus estimated EPS of the last four quarters.")
    static final class History extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.EARNINGS_HISTORY, ticker.earningsHistory());
        }
    }

    /** {@code estimates eps-trend}: {@link Ticker#epsTrend()}. */
    @Command(name = "eps-trend", description = "How the mean EPS estimates moved over the last 90 days.")
    static final class EpsTrend extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.EPS_TREND, ticker.epsTrend());
        }
    }

    /** {@code estimates eps-revisions}: {@link Ticker#epsRevisions()}. */
    @Command(name = "eps-revisions", description = "Upward and downward EPS estimate revisions.")
    static final class EpsRevisions extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.EPS_REVISIONS, ticker.epsRevisions());
        }
    }

    /** {@code estimates growth}: {@link Ticker#growthEstimates()}. */
    @Command(name = "growth", description = "Expected growth next to the industry, sector and index.")
    static final class Growth extends SymbolCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.GROWTH_ESTIMATE, ticker.growthEstimates());
        }
    }
}
