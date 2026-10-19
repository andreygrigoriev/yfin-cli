package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Screener;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.request.SortField;
import java.util.List;
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
 * {@code yfin screen NAME [--size N] [--offset N] [--sort FIELD [--asc]]}: the matches of one of
 * Yahoo's predefined screens ({@link Screener#predefined(String)}). {@code yfin screen --list}
 * prints the names of the predefined screens; a name outside that list is a usage error.
 */
@Command(name = "screen", description = "Run one of Yahoo's predefined stock and fund screens.")
public final class ScreenCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "NAME", arity = "0..1", description = "Predefined screen, e.g. day_gainers; see --list.")
    @Nullable
    String name;

    @Option(names = "--list", description = "Print the names of the predefined screens instead.")
    boolean list;

    @Option(
            names = "--size",
            paramLabel = "N",
            description = "Number of matches, 1 to " + Screener.MAX_SIZE + " (default: " + Screener.DEFAULT_SIZE + ").")
    @Nullable
    Integer size;

    @Option(
            names = "--offset",
            paramLabel = "N",
            description = "Index of the first match (default: 0). Runs the screen as its equivalent custom query.")
    @Nullable
    Integer offset;

    @Option(
            names = "--sort",
            paramLabel = "FIELD",
            description = "Yahoo screener field to sort by, e.g. percentchange or dayvolume (default: the"
                    + " screen's own order).")
    @Nullable
    String sort;

    @Option(names = "--asc", description = "Sort ascending instead of descending (with --sort).")
    boolean ascending;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        if (list) {
            if (name != null) {
                throw new ParameterException(spec.commandLine(), "--list takes no screen name");
            }
            root.output().list(Specs.SCREEN_NAME, List.copyOf(Screener.predefinedNames()));
            return 0;
        }
        Screener screener = screener(spec);
        root.output().list(Specs.SCREEN_QUOTE, root.client().screen(screener).quotes());
        return 0;
    }

    private Screener screener(CommandSpec spec) {
        if (name == null) {
            throw new ParameterException(spec.commandLine(), "Missing screen name; valid names: " + names());
        }
        String key = name.trim();
        if (!Screener.predefinedNames().contains(key)) {
            throw new ParameterException(
                    spec.commandLine(), "'" + name + "' is not a predefined screen; valid names: " + names());
        }
        if (ascending && sort == null) {
            throw new ParameterException(spec.commandLine(), "--asc needs --sort");
        }
        try {
            Screener screener = Screener.predefined(key);
            if (size != null) {
                screener = screener.size(size);
            }
            if (offset != null) {
                screener = screener.offset(offset);
            }
            if (sort != null) {
                screener = screener.sort(new SortField(sort.trim(), ascending));
            }
            return screener;
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private static String names() {
        return String.join(", ", Screener.predefinedNames());
    }
}
