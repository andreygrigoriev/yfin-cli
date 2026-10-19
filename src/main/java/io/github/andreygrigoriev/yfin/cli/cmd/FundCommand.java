package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.model.FundsData;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin fund SYMBOL [--holdings|--sectors]}: the {@link FundsData} overview of an ETF or
 * mutual fund as one record, or its top holdings or sector weights as a list. A symbol that is not
 * a fund prints an empty result.
 */
@Command(name = "fund", description = "Profile, fees and asset split of an ETF or mutual fund.")
public final class FundCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "SYMBOL", description = "Fund symbol, e.g. SPY.")
    String symbol = "";

    @ArgGroup(exclusive = true)
    @Nullable
    Section section;

    /** The list printed instead of the overview. */
    static final class Section {
        @Option(names = "--holdings", required = true, description = "Print the top holdings instead.")
        boolean holdings;

        @Option(names = "--sectors", required = true, description = "Print the sector weights instead.")
        boolean sectors;
    }

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Ticker ticker = Commands.ticker(spec, root.client(), symbol);
        Optional<FundsData> fund = ticker.fundsData();
        Output output = root.output();
        if (section != null && section.holdings) {
            output.list(Specs.FUND_HOLDING, fund.map(FundsData::topHoldings).orElse(List.of()));
        } else if (section != null && section.sectors) {
            output.list(
                    Specs.FUND_SECTOR,
                    fund.map(f -> List.copyOf(f.sectorWeightings().entrySet())).orElse(List.of()));
        } else {
            output.record(Specs.FUND, fund.orElse(null));
        }
        return 0;
    }
}
