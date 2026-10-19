package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.Tickers;
import io.github.andreygrigoriev.yfin.cli.MultiSymbol;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.model.NewsItem;
import io.github.andreygrigoriev.yfin.request.NewsTab;
import java.util.ArrayList;
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
 * {@code yfin news SYMBOL... [--count N] [--tab TAB]}: the latest news articles of each symbol,
 * newest first ({@link Ticker#news(int, NewsTab)}), one row per article with a leading
 * {@code symbol} column. A symbol without news prints no rows.
 */
@Command(name = "news", description = "Latest news articles of one or more symbols.")
public final class NewsCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "SYMBOL", description = "Ticker symbols, e.g. AAPL MSFT.")
    List<String> symbols = new ArrayList<>();

    @Option(
            names = "--count",
            paramLabel = "N",
            description = "Articles per symbol, at least 1 (default: ${DEFAULT-VALUE}).")
    int count = 10;

    @Option(names = "--tab", paramLabel = "TAB", description = "News tab: news, all or press-releases (default: news).")
    NewsTab tab = NewsTab.NEWS;

    @Override
    public Integer call() throws InterruptedException {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        if (count < 1) {
            throw new ParameterException(spec.commandLine(), "--count must be at least 1, was " + count);
        }
        YfinCommand root = YfinCommand.root(spec);
        Tickers tickers = Commands.tickers(spec, root.client(), symbols);
        MultiSymbol.Result<List<NewsItem>> result = MultiSymbol.fetch(
                List.copyOf(tickers.symbols()), symbol -> tickers.ticker(symbol).news(count, tab));
        List<Symbol<NewsItem>> rows = new ArrayList<>();
        for (Symbol<List<NewsItem>> news : result.successes()) {
            for (NewsItem item : news.value()) {
                rows.add(new Symbol<>(news.symbol(), item));
            }
        }
        root.output().list(Specs.NEWS.withSymbol(), rows);
        return result.reportFailures(spec.commandLine().getErr(), root.options().verbose());
    }
}
