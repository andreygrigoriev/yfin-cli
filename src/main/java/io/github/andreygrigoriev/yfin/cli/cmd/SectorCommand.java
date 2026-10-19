package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Sector;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
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
 * {@code yfin sector KEY [--industries|--top]}: the overview of a Yahoo sector as one record, or
 * its industries or largest companies as a list ({@link io.github.andreygrigoriev.yfin.YFinance#sector(String)}).
 */
@Command(name = "sector", description = "Overview, industries or top companies of a sector.")
public final class SectorCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "KEY", description = "Sector key, e.g. technology or financial-services.")
    String key = "";

    @ArgGroup(exclusive = true)
    @Nullable
    Section section;

    /** The list printed instead of the overview. */
    static final class Section {
        @Option(names = "--industries", required = true, description = "Print the sector's industries instead.")
        boolean industries;

        @Option(names = "--top", required = true, description = "Print the sector's largest companies instead.")
        boolean top;
    }

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Sector sector;
        try {
            sector = root.client().sector(key);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), "Invalid sector key: " + e.getMessage(), e);
        }
        Output output = root.output();
        if (section != null && section.industries) {
            output.list(Specs.SECTOR_INDUSTRY, sector.industries());
        } else if (section != null && section.top) {
            output.list(Specs.TOP_COMPANY, sector.topCompanies());
        } else {
            output.record(Specs.SECTOR, sector.data());
        }
        return 0;
    }
}
