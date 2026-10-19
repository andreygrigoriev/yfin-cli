package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.cli.output.Terminal;
import io.github.andreygrigoriev.yfin.exception.YFAuthException;
import io.github.andreygrigoriev.yfin.exception.YFDataException;
import io.github.andreygrigoriev.yfin.exception.YFDownloadException;
import io.github.andreygrigoriev.yfin.exception.YFRateLimitException;
import io.github.andreygrigoriev.yfin.exception.YFSymbolNotFoundException;
import io.github.andreygrigoriev.yfin.exception.YFTransportException;
import io.github.andreygrigoriev.yfin.exception.YFinanceException;
import io.github.andreygrigoriev.yfin.spi.Cache;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;
import picocli.CommandLine.Command;

class ErrorHandlerTest {
    private static final String NL = System.lineSeparator();

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    private int run(Exception thrown, String... args) {
        YfinCommand root = new YfinCommand(
                options -> YFinance.builder().cache(Cache.none()).build(), Terminal.piped(120));
        CommandLine commandLine = new CommandLine(root).addSubcommand(new Boom(thrown));
        return Converters.register(commandLine)
                .setExecutionExceptionHandler(new ErrorHandler())
                .setOut(new PrintWriter(out, true))
                .setErr(new PrintWriter(err, true))
                .execute(args);
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(new YFSymbolNotFoundException("NOPE"), 3, "yfin: NOPE: symbol not found"),
                Arguments.of(
                        new YFSymbolNotFoundException("NOPE", "no data for NOPE"), 3, "yfin: NOPE: no data for NOPE"),
                Arguments.of(new YFRateLimitException("too many requests"), 4, "yfin: too many requests"),
                Arguments.of(new YFAuthException("crumb rejected"), 5, "yfin: crumb rejected"),
                Arguments.of(new YFTransportException("connection reset"), 6, "yfin: connection reset"),
                Arguments.of(new YFDataException("malformed quote"), 7, "yfin: malformed quote"),
                Arguments.of(
                        new YFDownloadException(Map.of("MSFT", new YFTransportException("reset")), Map.of()),
                        7,
                        "yfin: download failed for 1 of 1 symbols: [MSFT]"),
                Arguments.of(new YFinanceException("generic"), 1, "yfin: generic"),
                Arguments.of(new InterruptedException(), 130, "yfin: interrupted"),
                Arguments.of(new IllegalStateException("bug"), 1, "yfin: java.lang.IllegalStateException: bug"),
                Arguments.of(new ExecutionException(new YFAuthException("expired")), 5, "yfin: expired"),
                Arguments.of(new CompletionException(new YFDataException("bad")), 7, "yfin: bad"));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void mapsEachFailureToItsCodeAndOneLine(Exception thrown, int code, String line) {
        assertThat(run(thrown, "boom")).isEqualTo(code);
        assertThat(err.toString()).isEqualTo(line + NL);
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void exitCodesMatchTheContract() {
        assertThat(ExitCodes.OK).isZero();
        assertThat(ExitCodes.UNEXPECTED).isEqualTo(1);
        assertThat(ExitCodes.USAGE).isEqualTo(2);
        assertThat(ExitCodes.of(new YFSymbolNotFoundException("X"))).isEqualTo(ExitCodes.SYMBOL_NOT_FOUND);
        assertThat(ExitCodes.of(new RuntimeException())).isEqualTo(ExitCodes.UNEXPECTED);
    }

    @Test
    void verboseAddsTheCauseChain() {
        Exception thrown = new YFTransportException(
                "request failed", new IOException("connection reset", new IOException("broken pipe")));
        assertThat(run(thrown, "boom", "--verbose")).isEqualTo(6);
        assertThat(err.toString())
                .isEqualTo("yfin: request failed" + NL
                        + "  caused by: java.io.IOException: connection reset" + NL
                        + "  caused by: java.io.IOException: broken pipe" + NL);
    }

    @Test
    void causeChainOnlyWithVerbose() {
        Exception thrown = new YFTransportException("request failed", new IOException("connection reset"));
        assertThat(run(thrown, "boom")).isEqualTo(6);
        assertThat(err.toString()).isEqualTo("yfin: request failed" + NL);
    }

    @Test
    void verbosePrintsTheStackTraceOfUnexpectedErrors() {
        assertThat(run(new IllegalStateException("bug"), "--verbose", "boom")).isEqualTo(1);
        assertThat(err.toString())
                .startsWith("yfin: java.lang.IllegalStateException: bug" + NL + "java.lang.IllegalStateException: bug")
                .contains("\tat ");
    }

    @Test
    void interruptRestoresTheFlag() {
        assertThat(run(new InterruptedException(), "boom")).isEqualTo(130);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void usageErrorsStayWithPicocli() {
        assertThat(run(new IllegalStateException("never thrown"), "boom", "--bogus"))
                .isEqualTo(2);
        assertThat(err.toString()).contains("Unknown option: '--bogus'");
    }

    @Test
    void reportPrefixesTheGivenSymbol() {
        StringWriter buffer = new StringWriter();
        int code = ErrorHandler.report(new PrintWriter(buffer), "MSFT", new YFDataException("no rows"), false);
        assertThat(code).isEqualTo(7);
        assertThat(buffer.toString()).isEqualTo("yfin: MSFT: no rows" + NL);
    }

    @Test
    void rootCommandLineUsesTheHandler() {
        YfinCommand root = new YfinCommand();
        assertThat(YfinCommand.commandLine(root).getExecutionExceptionHandler()).isInstanceOf(ErrorHandler.class);
    }

    @Command(name = "boom")
    static final class Boom implements java.util.concurrent.Callable<Integer> {
        private final Exception thrown;

        Boom(Exception thrown) {
            this.thrown = thrown;
        }

        @Override
        public Integer call() throws Exception {
            throw thrown;
        }
    }
}
