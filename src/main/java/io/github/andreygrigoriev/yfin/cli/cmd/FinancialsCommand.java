package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.StatementSpecs;
import io.github.andreygrigoriev.yfin.model.FinancialStatement;
import io.github.andreygrigoriev.yfin.request.Frequency;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin financials income|balance|cashflow SYMBOL}: a financial statement as line items by
 * period, newest period first. Annual by default; {@code --quarterly} for fiscal quarters and, for
 * the income and cash flow statements, {@code --ttm} for the latest trailing twelve months (Yahoo
 * has no trailing balance sheet). A symbol without fundamentals (an ETF, say) prints an empty
 * result.
 */
@Command(
        name = "financials",
        description = "Financial statements: income, balance sheet, cash flow.",
        subcommands = {FinancialsCommand.Income.class, FinancialsCommand.Balance.class, FinancialsCommand.CashFlow.class
        })
public final class FinancialsCommand implements Runnable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Override
    public void run() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /** What the statement subcommands share: the symbol, fetching and printing. */
    abstract static class StatementCommand implements Callable<Integer> {
        @Spec
        @Nullable
        CommandSpec spec;

        @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
        String symbol = "";

        /** The reporting period the options select. */
        abstract Frequency frequency();

        /** Fetches the statement. */
        abstract FinancialStatement fetch(Ticker ticker, Frequency frequency);

        @Override
        public Integer call() {
            CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
            YfinCommand root = YfinCommand.root(spec);
            Ticker ticker = Commands.ticker(spec, root.client(), symbol);
            FinancialStatement statement = fetch(ticker, frequency());
            root.output().list(StatementSpecs.of(statement), StatementSpecs.rows(statement));
            return 0;
        }
    }

    /** {@code financials income}: {@link Ticker#incomeStatement(Frequency)}. */
    @Command(name = "income", description = "Income statement, newest period first.")
    static final class Income extends StatementCommand {
        @ArgGroup(exclusive = true)
        @Nullable
        PeriodOptions period;

        @Override
        Frequency frequency() {
            return PeriodOptions.frequency(period);
        }

        @Override
        FinancialStatement fetch(Ticker ticker, Frequency frequency) {
            return ticker.incomeStatement(frequency);
        }
    }

    /** {@code financials balance}: {@link Ticker#balanceSheet(Frequency)}; no trailing variant. */
    @Command(name = "balance", description = "Balance sheet, newest period first.")
    static final class Balance extends StatementCommand {
        @Option(names = "--quarterly", description = "Fiscal quarters instead of fiscal years.")
        boolean quarterly;

        @Override
        Frequency frequency() {
            return quarterly ? Frequency.QUARTERLY : Frequency.ANNUAL;
        }

        @Override
        FinancialStatement fetch(Ticker ticker, Frequency frequency) {
            return ticker.balanceSheet(frequency);
        }
    }

    /** {@code financials cashflow}: {@link Ticker#cashFlow(Frequency)}. */
    @Command(name = "cashflow", description = "Cash flow statement, newest period first.")
    static final class CashFlow extends StatementCommand {
        @ArgGroup(exclusive = true)
        @Nullable
        PeriodOptions period;

        @Override
        Frequency frequency() {
            return PeriodOptions.frequency(period);
        }

        @Override
        FinancialStatement fetch(Ticker ticker, Frequency frequency) {
            return ticker.cashFlow(frequency);
        }
    }
}
