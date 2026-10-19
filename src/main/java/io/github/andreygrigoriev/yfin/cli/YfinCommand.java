package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.cmd.ActionsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.CapitalGainsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.DividendsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.EarningsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.EstimatesCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.EventsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.FinancialsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.FundCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.HistoryCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.HoldersCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.InfoCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.IsinCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.PriceTargetCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.QuoteCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.RecommendationsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.SecFilingsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.SharesCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.SplitsCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.SustainabilityCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.UpgradesCommand;
import io.github.andreygrigoriev.yfin.cli.cmd.ValuationCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

/**
 * Root {@code yfin} command. Subcommands hang off it; on its own it only reports usage.
 *
 * <p>It owns the state of one invocation: the {@link GlobalOptions}, the {@link YFinance} client,
 * created on first use and released by {@link #close()}, and the {@link Output}.
 */
@Command(
        name = "yfin",
        mixinStandardHelpOptions = true,
        scope = ScopeType.INHERIT,
        subcommands = {
            QuoteCommand.class,
            HistoryCommand.class,
            InfoCommand.class,
            IsinCommand.class,
            EventsCommand.class,
            SustainabilityCommand.class,
            FundCommand.class,
            DividendsCommand.class,
            SplitsCommand.class,
            CapitalGainsCommand.class,
            ActionsCommand.class,
            SharesCommand.class,
            SecFilingsCommand.class,
            FinancialsCommand.class,
            ValuationCommand.class,
            HoldersCommand.class,
            EstimatesCommand.class,
            RecommendationsCommand.class,
            UpgradesCommand.class,
            PriceTargetCommand.class,
            EarningsCommand.class
        },
        versionProvider = YfinCommand.VersionProvider.class,
        description = "Yahoo Finance market data from the command line.")
public final class YfinCommand implements Runnable, AutoCloseable {
    @Spec
    @Nullable
    CommandSpec spec;

    @Mixin
    GlobalOptions options = new GlobalOptions();

    private final ClientFactory clientFactory;
    private final Terminal terminal;
    private @Nullable YFinance client;

    /** A root command over the real network and the process terminal. */
    public YfinCommand() {
        this(ClientFactory.DEFAULT, Terminal.detect());
    }

    /**
     * A root command with an explicit client factory and terminal, as tests use.
     *
     * @param clientFactory builds the client on first use
     * @param terminal where stdout goes
     */
    public YfinCommand(ClientFactory clientFactory, Terminal terminal) {
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        this.terminal = Objects.requireNonNull(terminal, "terminal");
    }

    /**
     * The command line for {@code root}, with its converters and {@link ErrorHandler} registered.
     *
     * @param root the root command
     * @return the command line
     */
    public static CommandLine commandLine(YfinCommand root) {
        return Converters.register(new CommandLine(root)).setExecutionExceptionHandler(new ErrorHandler());
    }

    /**
     * The root command of the command tree {@code spec} belongs to.
     *
     * @param spec the spec of any command in the tree
     * @return the root command
     */
    public static YfinCommand root(CommandSpec spec) {
        return (YfinCommand) spec.root().userObject();
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(commandSpec().commandLine(), "Missing required subcommand");
    }

    /** The parsed global options. */
    public GlobalOptions options() {
        return options;
    }

    /**
     * The client of this invocation, built from the global options on first call.
     *
     * @return the client
     */
    public synchronized YFinance client() {
        if (client == null) {
            client = clientFactory.create(options);
        }
        return client;
    }

    /**
     * An output in the chosen format, writing to the command line's stdout and stderr.
     *
     * @return the output
     */
    public Output output() {
        CommandLine commandLine = commandSpec().commandLine();
        return new Output(options.format(), terminal, options.noColor(), commandLine.getOut(), commandLine.getErr());
    }

    /** Closes the client, if one was created. */
    @Override
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private CommandSpec commandSpec() {
        return Objects.requireNonNull(spec, "spec is injected by picocli");
    }

    /** Prints the CLI version and the yfin-core version it was built with. */
    static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"yfin " + BuildInfo.VERSION, "yfin-core " + BuildInfo.YFIN_CORE_VERSION};
        }
    }
}
