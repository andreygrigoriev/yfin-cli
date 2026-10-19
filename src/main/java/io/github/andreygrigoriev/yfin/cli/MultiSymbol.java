package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Runs one call per symbol, concurrently on virtual threads. The client's request limiter bounds
 * the actual concurrency. A failing symbol doesn't stop the others.
 */
public final class MultiSymbol {
    private MultiSymbol() {}

    /**
     * Calls {@code fn} for every symbol and collects the outcomes in input order.
     *
     * @param symbols the symbols, in output order
     * @param fn the call for one symbol
     * @param <T> result type
     * @return successes and failures, each in input order
     * @throws InterruptedException if interrupted while waiting; the pending calls are cancelled
     */
    public static <T> Result<T> fetch(List<String> symbols, Function<String, T> fn) throws InterruptedException {
        Objects.requireNonNull(fn, "fn");
        List<Symbol<T>> successes = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>(symbols.size());
            for (String symbol : symbols) {
                futures.add(executor.submit(() -> fn.apply(symbol)));
            }
            try {
                for (int i = 0; i < symbols.size(); i++) {
                    String symbol = symbols.get(i);
                    try {
                        successes.add(new Symbol<>(symbol, futures.get(i).get()));
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause() == null ? e : e.getCause();
                        if (cause instanceof Error error) {
                            throw error;
                        }
                        failures.add(new Failure(symbol, cause));
                    }
                }
            } catch (InterruptedException | Error e) {
                executor.shutdownNow();
                throw e;
            }
        }
        return new Result<>(successes, failures);
    }

    /**
     * The outcome of {@link #fetch}.
     *
     * @param successes the symbols that returned a value, in input order
     * @param failures the symbols that failed, in input order
     * @param <T> result type
     */
    public record Result<T>(List<Symbol<T>> successes, List<Failure> failures) {
        /** Copies both lists. */
        public Result {
            successes = List.copyOf(successes);
            failures = List.copyOf(failures);
        }

        /**
         * The successful values without their symbols.
         *
         * @return the values, in input order
         */
        public List<T> values() {
            return successes.stream().map(Symbol::value).toList();
        }

        /**
         * The exit code of the first failure, {@link ExitCodes#OK} if nothing failed.
         *
         * @return the exit code
         */
        public int exitCode() {
            return failures.isEmpty()
                    ? ExitCodes.OK
                    : ExitCodes.of(failures.getFirst().error());
        }

        /**
         * Writes one diagnostic per failure to {@code err}.
         *
         * @param err where diagnostics go
         * @param verbose whether to add cause chains
         * @return {@link #exitCode()}
         */
        public int reportFailures(PrintWriter err, boolean verbose) {
            for (Failure failure : failures) {
                ErrorHandler.report(err, failure.symbol(), failure.error(), verbose);
            }
            return exitCode();
        }
    }

    /**
     * A symbol whose call failed.
     *
     * @param symbol the symbol
     * @param error what it threw
     */
    public record Failure(String symbol, Throwable error) {
        /** Rejects nulls. */
        public Failure {
            Objects.requireNonNull(symbol, "symbol");
            Objects.requireNonNull(error, "error");
        }
    }
}
