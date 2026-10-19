package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Market;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.request.MarketRegion;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code yfin market REGION [--status]}: the headline instruments of a market region as a list, or
 * today's trading hours as one record ({@link io.github.andreygrigoriev.yfin.YFinance#market(MarketRegion)}).
 * Yahoo only reports trading hours for {@code us}; other regions print an empty status.
 */
@Command(name = "market", description = "Headline instruments or trading hours of a market region.")
public final class MarketCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(
            paramLabel = "REGION",
            description =
                    "Market region: us, gb, asia, europe, rates, commodities, currencies or" + " cryptocurrencies.")
    MarketRegion region = MarketRegion.US;

    @Option(names = "--status", description = "Print today's trading hours instead (us only).")
    boolean status;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        YfinCommand root = YfinCommand.root(spec);
        Market market = root.client().market(region);
        if (status) {
            root.output().record(Specs.MARKET_STATUS, market.status().orElse(null));
        } else {
            root.output().list(Specs.MARKET_SUMMARY, market.summary());
        }
        return 0;
    }
}
