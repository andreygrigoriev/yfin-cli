package io.github.andreygrigoriev.yfin.cli.testing;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.ClientFactory;
import io.github.andreygrigoriev.yfin.cli.YfinCommand;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import io.github.andreygrigoriev.yfin.spi.Cache;
import io.github.andreygrigoriev.yfin.testing.FakeTransport;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

/**
 * Runs {@code yfin} in-process over a {@link FakeTransport}, with fixed settings: a piped terminal
 * {@value #WIDTH} columns wide, no cache and no retries unless the arguments say otherwise.
 *
 * <p>The cookie and crumb bootstrap is registered up front; tests add the Yahoo responses the
 * command needs, usually from library fixtures:
 *
 * <pre>{@code
 * CliRunner cli = new CliRunner();
 * cli.onGet("/v7/finance/quote").respond(200, Fixtures.load("quote/aapl.json"));
 * Golden.assertMatches(cli.run("quote", "AAPL"), "quote/normal.table");
 * }</pre>
 */
public final class CliRunner {
    /** Terminal width of every run. */
    public static final int WIDTH = 120;

    /** URI path of the fundamentals timeseries endpoint, without the symbol. */
    public static final String TIMESERIES = "/ws/fundamentals-timeseries/v1/finance/timeseries/";

    private final FakeTransport transport = FakeTransport.create();

    /** A runner with the cookie and crumb bootstrap registered. */
    public CliRunner() {
        transport.onGet("").respond(404, "not found");
        transport.onGet("/v1/test/getcrumb").respond(200, "crumb1");
    }

    /**
     * Starts a GET rule on the fake transport.
     *
     * @param path exact URI path
     * @return the rule builder
     */
    public FakeTransport.RuleBuilder onGet(String path) {
        return transport.onGet(path);
    }

    /**
     * Starts a POST rule on the fake transport.
     *
     * @param path exact URI path
     * @return the rule builder
     */
    public FakeTransport.RuleBuilder onPost(String path) {
        return transport.onPost(path);
    }

    /**
     * Answers GET {@code path} with status 200 and a library fixture.
     *
     * @param path exact URI path
     * @param fixture fixture path below {@code fixtures/}
     * @return this runner
     */
    public CliRunner fixture(String path, String fixture) {
        transport.onGet(path).respond(200, Fixtures.load(fixture));
        return this;
    }

    /**
     * Answers the chunked timeseries requests of {@code symbol} with the library fixtures {@code
     * <prefix>-1.json}, {@code <prefix>-2.json}, ... The library asks for a statement's types in
     * chunks and Yahoo echoes every requested type in its {@code meta}, so a chunk request is
     * matched to the fixture whose types include the first type it asks for.
     *
     * @param symbol the symbol in the URI path
     * @param prefix fixture path below {@code fixtures/} without the {@code -<n>.json} suffix, e.g.
     *     {@code timeseries/aapl-annual-financials}
     * @return this runner
     * @throws AssertionError if there is no {@code <prefix>-1.json}
     */
    public CliRunner timeseries(String symbol, String prefix) {
        for (int n = 1; n == 1 || Fixtures.exists(prefix + "-" + n + ".json"); n++) {
            String body = Fixtures.load(prefix + "-" + n + ".json");
            Set<String> types = Fixtures.timeseriesTypes(body);
            transport
                    .onGet(TIMESERIES + symbol)
                    .queryMatching(q -> q.containsKey("type")
                            && types.contains(q.get("type").getFirst().split(",")[0]))
                    .respond(200, body);
        }
        return this;
    }

    /** The fake transport, for request assertions. */
    public FakeTransport transport() {
        return transport;
    }

    /**
     * Runs {@code yfin} with {@code args} and captures its output. Each run gets a new root
     * command and client; the client is closed afterwards.
     *
     * @param args command-line arguments
     * @return exit code, stdout and stderr
     */
    public CliResult run(String... args) {
        ClientFactory factory = options -> ClientFactory.configure(
                        YFinance.builder()
                                .transport(transport)
                                .cache(Cache.none())
                                .maxRetries(0),
                        options)
                .build();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode;
        try (YfinCommand root = new YfinCommand(factory, Terminal.piped(WIDTH))) {
            exitCode = YfinCommand.commandLine(root)
                    .setOut(new PrintWriter(out, true))
                    .setErr(new PrintWriter(err, true))
                    .execute(args);
        }
        return new CliResult(exitCode, out.toString(), err.toString());
    }
}
