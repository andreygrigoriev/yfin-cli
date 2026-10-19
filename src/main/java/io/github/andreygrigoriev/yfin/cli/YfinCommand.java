package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
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
