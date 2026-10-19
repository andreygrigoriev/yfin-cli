package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.OptionRow;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.model.OptionChain;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * {@code yfin options SYMBOL [--expiry DATE [--calls|--puts]]}: without {@code --expiry}, the
 * option expiration dates, nearest first ({@link Ticker#optionExpirations()}); with it, the calls
 * and puts of that expiration ({@link Ticker#optionChain(LocalDate)}). A symbol without listed
 * options, including an unknown one, has no expirations; a date that isn't one of them is a usage
 * error listing the valid dates.
 */
@Command(name = "options", description = "Option expiration dates, or the option chain of one expiration.")
public final class OptionsCommand extends SymbolCommand {
    @Option(
            names = "--expiry",
            paramLabel = "DATE",
            description = "Expiration date (YYYY-MM-DD) whose contracts to print; without it, the expirations.")
    @Nullable
    LocalDate expiry;

    @ArgGroup(exclusive = true)
    @Nullable
    Side side;

    /** The mutually exclusive {@code --calls|--puts} options; neither means both. */
    static final class Side {
        @Option(names = "--calls", required = true, description = "Only the calls.")
        boolean calls;

        @Option(names = "--puts", required = true, description = "Only the puts.")
        boolean puts;
    }

    @Override
    public Integer call() {
        if (side != null && expiry == null) {
            throw new ParameterException(commandSpec().commandLine(), "--calls and --puts need --expiry");
        }
        return super.call();
    }

    @Override
    void print(Ticker ticker, Output output) {
        LocalDate expiry = this.expiry;
        if (expiry == null) {
            output.list(Specs.OPTION_EXPIRATION, ticker.optionExpirations());
            return;
        }
        OptionChain chain;
        try {
            chain = ticker.optionChain(expiry);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(commandSpec().commandLine(), e.getMessage(), e);
        }
        boolean puts = side == null || side.puts;
        boolean calls = side == null || side.calls;
        output.list(Specs.OPTION_CONTRACT, OptionRow.of(chain, calls, puts));
    }

    private CommandSpec commandSpec() {
        return Objects.requireNonNull(spec, "spec is injected by picocli");
    }
}
