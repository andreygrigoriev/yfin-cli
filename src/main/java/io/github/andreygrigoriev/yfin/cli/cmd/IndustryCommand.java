package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Industry;
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
 * {@code yfin industry KEY [--top|--performing|--growth]}: the overview of a Yahoo industry as one
 * record, or one of its company lists ({@link io.github.andreygrigoriev.yfin.YFinance#industry(String)}).
 */
@Command(name = "industry", description = "Overview or top companies of an industry.")
public final class IndustryCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "KEY", description = "Industry key, e.g. semiconductors; see sector --industries.")
    String key = "";

    @ArgGroup(exclusive = true)
    @Nullable
    Section section;

    /** The list printed instead of the overview. */
    static final class Section {
        @Option(names = "--top", required = true, description = "Print the largest companies instead.")
        boolean top;

        @Option(names = "--performing", required = true, description = "Print the best performers this year instead.")
        boolean performing;

        @Option(names = "--growth", required = true, description = "Print the fastest expected growers instead.")
        boolean growth;
    }

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Industry industry;
        try {
            industry = root.client().industry(key);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), "Invalid industry key: " + e.getMessage(), e);
        }
        Output output = root.output();
        if (section != null && section.top) {
            output.list(Specs.TOP_COMPANY, industry.topCompanies());
        } else if (section != null && section.performing) {
            output.list(Specs.PERFORMING_COMPANY, industry.topPerformingCompanies());
        } else if (section != null && section.growth) {
            output.list(Specs.GROWTH_COMPANY, industry.topGrowthCompanies());
        } else {
            output.record(Specs.INDUSTRY, industry.data());
        }
        return 0;
    }
}
