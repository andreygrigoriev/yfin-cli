package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Tickers;
import io.github.andreygrigoriev.yfin.YFinance;
import java.util.List;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/** Helpers shared by the commands. */
final class Commands {
    private Commands() {}

    /**
     * The symbols as a library group: normalized and deduplicated, in input order.
     *
     * @param spec the command, for the usage error
     * @param client the client
     * @param symbols the symbols as given
     * @return the group
     * @throws ParameterException if a symbol is blank or otherwise invalid (exit 2)
     */
    static Tickers tickers(CommandSpec spec, YFinance client, List<String> symbols) {
        try {
            return client.tickers(symbols);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), "Invalid symbol: " + e.getMessage());
        }
    }
}
