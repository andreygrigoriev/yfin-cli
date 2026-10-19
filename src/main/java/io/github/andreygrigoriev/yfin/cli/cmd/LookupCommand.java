package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Lookup;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.request.LookupType;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin lookup QUERY [--type TYPE] [--count N]}: instruments matching a query, optionally of
 * one type, in Yahoo's order ({@link io.github.andreygrigoriev.yfin.YFinance#lookup(Lookup)}).
 */
@Command(name = "lookup", description = "Find instruments of a given type (stock, ETF, crypto, ...) by query.")
public final class LookupCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "QUERY", description = "Name or symbol, e.g. gold.")
    String query = "";

    @Option(
            names = "--type",
            paramLabel = "TYPE",
            description = "Instrument type: all, stock, mutual-fund, etf, index, future, currency or"
                    + " cryptocurrency (default: all).")
    LookupType type = LookupType.ALL;

    @Option(
            names = "--count",
            paramLabel = "N",
            description = "Maximum number of matches, at least 1 (default: ${DEFAULT-VALUE}).")
    int count = Lookup.DEFAULT_COUNT;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        Lookup lookup;
        try {
            lookup = Lookup.of(query).type(type).count(count);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
        YfinCommand root = YfinCommand.root(spec);
        root.output().list(Specs.LOOKUP_RESULT, root.client().lookup(lookup));
        return 0;
    }
}
