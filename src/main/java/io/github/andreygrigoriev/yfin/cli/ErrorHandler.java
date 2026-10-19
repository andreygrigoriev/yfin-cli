package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.exception.YFSymbolNotFoundException;
import io.github.andreygrigoriev.yfin.exception.YFinanceException;
import java.io.PrintWriter;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * Turns an exception thrown by a command into a stderr line and an exit code from {@link ExitCodes}.
 *
 * <p>The line is {@code yfin: <symbol>: <reason>}, or {@code yfin: <reason>} when no symbol is
 * known. With {@code --verbose} the cause chain follows, one {@code caused by} line per cause; an
 * unexpected exception (anything outside the {@link YFinanceException} hierarchy) prints its stack
 * trace instead. A plain {@link YFinanceException} exits with {@link ExitCodes#UNEXPECTED} but is
 * reported like the other library errors.
 */
public final class ErrorHandler implements IExecutionExceptionHandler {
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
        boolean verbose = commandLine.getCommandSpec().root().userObject() instanceof YfinCommand root
                && root.options().verbose();
        return report(commandLine.getErr(), null, ex, verbose);
    }

    /**
     * Writes the diagnostics of a failure to {@code err}.
     *
     * @param err where diagnostics go
     * @param symbol the symbol the failure belongs to, {@code null} if unknown
     * @param error the failure
     * @param verbose whether to add the cause chain or stack trace
     * @return the exit code of the failure
     */
    public static int report(PrintWriter err, @Nullable String symbol, Throwable error, boolean verbose) {
        Throwable failure = unwrap(error);
        int code = ExitCodes.of(failure);
        if (symbol == null && failure instanceof YFSymbolNotFoundException notFound) {
            symbol = notFound.symbol();
        }
        String prefix = symbol == null ? "yfin: " : "yfin: " + symbol + ": ";
        if (code == ExitCodes.INTERRUPTED) {
            Thread.currentThread().interrupt();
            err.println(prefix + "interrupted");
        } else if (code == ExitCodes.UNEXPECTED && !(failure instanceof YFinanceException)) {
            err.println(prefix + failure);
            if (verbose) {
                failure.printStackTrace(err);
            }
        } else {
            err.println(prefix + reason(symbol, failure));
            if (verbose) {
                for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
                    err.println("  caused by: " + cause);
                }
            }
        }
        err.flush();
        return code;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String reason(@Nullable String symbol, Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure instanceof YFinanceException ? failure.getClass().getSimpleName() : failure.toString();
        }
        // "symbol not found: AAPL" would repeat the symbol already in the prefix
        String suffix = ": " + symbol;
        if (symbol != null && message.endsWith(suffix) && message.length() > suffix.length()) {
            return message.substring(0, message.length() - suffix.length());
        }
        return message;
    }
}
