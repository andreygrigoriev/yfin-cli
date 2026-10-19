package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin holders major|institutional|funds|insiders|insider-tx SYMBOL}: who owns the symbol.
 * {@code major} is the ownership breakdown as one record, the others are lists in Yahoo's order. A
 * symbol without fundamentals (an ETF, a crypto pair) prints an empty result.
 */
@Command(
        name = "holders",
        description = "Ownership: breakdown, institutions, funds, insiders and their transactions.",
        subcommands = {
            HoldersCommand.Major.class,
            HoldersCommand.Institutional.class,
            HoldersCommand.Funds.class,
            HoldersCommand.Insiders.class,
            HoldersCommand.InsiderTransactions.class
        })
public final class HoldersCommand implements Runnable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Override
    public void run() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    /** What the holder subcommands share: the symbol, the client and the output. */
    abstract static class HolderCommand implements Callable<Integer> {
        @Spec
        @Nullable
        CommandSpec spec;

        @Parameters(paramLabel = "SYMBOL", description = "Ticker symbol, e.g. AAPL.")
        String symbol = "";

        /** Fetches and prints the data. */
        abstract void print(Ticker ticker, Output output);

        @Override
        public Integer call() {
            CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
            YfinCommand root = YfinCommand.root(spec);
            print(Commands.ticker(spec, root.client(), symbol), root.output());
            return 0;
        }
    }

    /** {@code holders major}: {@link Ticker#majorHolders()}. */
    @Command(name = "major", description = "Ownership breakdown: insiders and institutions.")
    static final class Major extends HolderCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.record(Specs.MAJOR_HOLDERS, ticker.majorHolders().orElse(null));
        }
    }

    /** {@code holders institutional}: {@link Ticker#institutionalHolders()}. */
    @Command(name = "institutional", description = "Top institutional holders.")
    static final class Institutional extends HolderCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.HOLDER, ticker.institutionalHolders());
        }
    }

    /** {@code holders funds}: {@link Ticker#mutualFundHolders()}. */
    @Command(name = "funds", description = "Top mutual fund holders.")
    static final class Funds extends HolderCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.HOLDER, ticker.mutualFundHolders());
        }
    }

    /** {@code holders insiders}: {@link Ticker#insiderRosterHolders()}. */
    @Command(name = "insiders", description = "Insiders and their current positions.")
    static final class Insiders extends HolderCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.INSIDER, ticker.insiderRosterHolders());
        }
    }

    /** {@code holders insider-tx}: {@link Ticker#insiderTransactions()}. */
    @Command(name = "insider-tx", description = "Recent insider transactions, newest first.")
    static final class InsiderTransactions extends HolderCommand {
        @Override
        void print(Ticker ticker, Output output) {
            output.list(Specs.INSIDER_TRANSACTION, ticker.insiderTransactions());
        }
    }
}
