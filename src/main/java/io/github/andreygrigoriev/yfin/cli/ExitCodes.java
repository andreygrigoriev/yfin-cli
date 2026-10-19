package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.exception.YFAuthException;
import io.github.andreygrigoriev.yfin.exception.YFDataException;
import io.github.andreygrigoriev.yfin.exception.YFDownloadException;
import io.github.andreygrigoriev.yfin.exception.YFRateLimitException;
import io.github.andreygrigoriev.yfin.exception.YFSymbolNotFoundException;
import io.github.andreygrigoriev.yfin.exception.YFTransportException;

/** The process exit codes of {@code yfin}. They are part of the CLI contract and never change. */
public final class ExitCodes {
    /** Success, including an empty result. */
    public static final int OK = 0;
    /** An unexpected exception. */
    public static final int UNEXPECTED = 1;
    /** A usage or conversion error, as reported by picocli. */
    public static final int USAGE = 2;
    /** {@link YFSymbolNotFoundException}. */
    public static final int SYMBOL_NOT_FOUND = 3;
    /** {@link YFRateLimitException}. */
    public static final int RATE_LIMIT = 4;
    /** {@link YFAuthException}. */
    public static final int AUTH = 5;
    /** {@link YFTransportException}. */
    public static final int TRANSPORT = 6;
    /** {@link YFDataException} and {@link YFDownloadException}. */
    public static final int DATA = 7;
    /** The invocation was interrupted. */
    public static final int INTERRUPTED = 130;

    private ExitCodes() {}

    /**
     * The exit code for a failure.
     *
     * @param error the failure
     * @return its exit code
     */
    public static int of(Throwable error) {
        return switch (error) {
            case YFSymbolNotFoundException _ -> SYMBOL_NOT_FOUND;
            case YFRateLimitException _ -> RATE_LIMIT;
            case YFAuthException _ -> AUTH;
            case YFTransportException _ -> TRANSPORT;
            case YFDataException _, YFDownloadException _ -> DATA;
            case InterruptedException _ -> INTERRUPTED;
            default -> UNEXPECTED;
        };
    }
}
