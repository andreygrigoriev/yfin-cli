package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.exception.YFDataException;
import io.github.andreygrigoriev.yfin.exception.YFRateLimitException;
import io.github.andreygrigoriev.yfin.exception.YFSymbolNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MultiSymbolTest {
    private static final String NL = System.lineSeparator();

    @Test
    void allSucceedInInputOrder() throws InterruptedException {
        // the first symbol finishes last, so the order can't come from completion order
        CountDownLatch othersDone = new CountDownLatch(2);
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of("AAPL", "MSFT", "GOOG"), symbol -> {
            if (symbol.equals("AAPL")) {
                await(othersDone);
            } else {
                othersDone.countDown();
            }
            return symbol.toLowerCase(Locale.ROOT);
        });

        assertThat(result.successes())
                .containsExactly(
                        new Symbol<>("AAPL", "aapl"), new Symbol<>("MSFT", "msft"), new Symbol<>("GOOG", "goog"));
        assertThat(result.values()).containsExactly("aapl", "msft", "goog");
        assertThat(result.failures()).isEmpty();
        assertThat(result.exitCode()).isZero();

        StringWriter err = new StringWriter();
        assertThat(result.reportFailures(new PrintWriter(err), false)).isZero();
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void callsRunConcurrently() throws InterruptedException {
        CountDownLatch allStarted = new CountDownLatch(3);
        Set<String> threads = ConcurrentHashMap.newKeySet();
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of("A", "B", "C"), symbol -> {
            threads.add(Thread.currentThread().toString());
            assertThat(Thread.currentThread().isVirtual()).isTrue();
            allStarted.countDown();
            await(allStarted);
            return symbol;
        });
        assertThat(result.values()).containsExactly("A", "B", "C");
        assertThat(threads).hasSize(3);
    }

    @Test
    void partialFailureKeepsSuccessesAndReportsFirstFailure() throws InterruptedException {
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of("AAPL", "NOPE", "MSFT", "LIMIT"), symbol -> {
            switch (symbol) {
                case "NOPE" -> throw new YFSymbolNotFoundException("NOPE");
                case "LIMIT" -> throw new YFRateLimitException("too many requests");
                default -> {
                    return symbol;
                }
            }
        });

        assertThat(result.values()).containsExactly("AAPL", "MSFT");
        assertThat(result.failures()).extracting(MultiSymbol.Failure::symbol).containsExactly("NOPE", "LIMIT");
        assertThat(result.exitCode()).isEqualTo(ExitCodes.SYMBOL_NOT_FOUND);

        StringWriter err = new StringWriter();
        assertThat(result.reportFailures(new PrintWriter(err), false)).isEqualTo(3);
        assertThat(err.toString())
                .isEqualTo("yfin: NOPE: symbol not found" + NL + "yfin: LIMIT: too many requests" + NL);
    }

    @Test
    void allFail() throws InterruptedException {
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of("X", "Y"), symbol -> {
            throw new YFDataException("malformed payload for " + symbol);
        });

        assertThat(result.successes()).isEmpty();
        assertThat(result.exitCode()).isEqualTo(ExitCodes.DATA);

        StringWriter err = new StringWriter();
        assertThat(result.reportFailures(new PrintWriter(err), false)).isEqualTo(7);
        assertThat(err.toString())
                .isEqualTo("yfin: X: malformed payload for X" + NL + "yfin: Y: malformed payload for Y" + NL);
    }

    @Test
    void unexpectedFailureIsCollectedToo() throws InterruptedException {
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of("A"), symbol -> {
            throw new IllegalStateException("bug");
        });
        assertThat(result.exitCode()).isEqualTo(ExitCodes.UNEXPECTED);
        assertThat(result.failures().getFirst().error()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void errorsPropagate() {
        assertThatThrownBy(() -> MultiSymbol.fetch(List.of("A"), symbol -> {
                    throw new AssertionError("fatal");
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessage("fatal");
    }

    @Test
    void noSymbolsGivesAnEmptyResult() throws InterruptedException {
        MultiSymbol.Result<String> result = MultiSymbol.fetch(List.of(), symbol -> symbol);
        assertThat(result.successes()).isEmpty();
        assertThat(result.failures()).isEmpty();
        assertThat(result.exitCode()).isZero();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
