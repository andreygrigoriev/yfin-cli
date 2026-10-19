package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Search;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.output.Section;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import io.github.andreygrigoriev.yfin.model.SearchResult;
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
 * {@code yfin search QUERY [--max N] [--news N]}: symbols matching a name or symbol, best match
 * first ({@link io.github.andreygrigoriev.yfin.YFinance#search(Search)}). With {@code --news N}
 * above 0 it also prints up to N news articles: a second list in table and CSV, and a
 * {@code {"quotes": [...], "news": [...]}} object in JSON.
 */
@Command(name = "search", description = "Find symbols by company name or symbol, optionally with news.")
public final class SearchCommand implements Callable<Integer> {
    @Spec
    @Nullable
    CommandSpec spec;

    @Parameters(paramLabel = "QUERY", description = "Company name or symbol, e.g. apple.")
    String query = "";

    @Option(
            names = "--max",
            paramLabel = "N",
            description = "Maximum number of matches, 0 for none (default: ${DEFAULT-VALUE}).")
    int max = 8;

    @Option(
            names = "--news",
            paramLabel = "N",
            description = "Number of news articles to add, 0 for none (default: ${DEFAULT-VALUE}).")
    int news = 0;

    @Override
    public Integer call() {
        CommandSpec spec = Objects.requireNonNull(this.spec, "spec is injected by picocli");
        if (max < 0) {
            throw new ParameterException(spec.commandLine(), "--max must not be negative, was " + max);
        }
        if (news < 0) {
            throw new ParameterException(spec.commandLine(), "--news must not be negative, was " + news);
        }
        Search search;
        try {
            search = Search.of(query).maxResults(max).newsCount(news).listsCount(0);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), "Invalid query: " + e.getMessage(), e);
        }
        YfinCommand root = YfinCommand.root(spec);
        SearchResult result = root.client().search(search);
        Output output = root.output();
        if (news == 0) {
            output.list(Specs.SEARCH_QUOTE, result.quotes());
        } else {
            output.lists(List.of(
                    new Section<>("quotes", Specs.SEARCH_QUOTE, result.quotes()),
                    new Section<>("news", Specs.NEWS, result.news())));
        }
        return 0;
    }
}
