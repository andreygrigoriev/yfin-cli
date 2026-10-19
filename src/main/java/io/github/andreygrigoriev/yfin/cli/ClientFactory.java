package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.YFinance;
import io.github.andreygrigoriev.yfin.spi.Cache;
import java.time.Duration;
import java.util.Objects;

/** Builds the {@link YFinance} client of one invocation. Tests swap in one over a fake transport. */
@FunctionalInterface
public interface ClientFactory {
    /** Builds a client over the real network, configured from the global options. */
    ClientFactory DEFAULT = options -> configure(YFinance.builder(), options).build();

    /**
     * Builds a client.
     *
     * @param options the parsed global options
     * @return a new client; the caller closes it
     */
    YFinance create(GlobalOptions options);

    /**
     * Applies the global options to a builder: {@code --no-cache} disables the cache, {@code
     * --timeout} sets the request timeout and {@code --retries} the retry count. Options left out
     * keep the library defaults.
     *
     * @param builder the builder to configure
     * @param options the parsed global options
     * @return {@code builder}
     */
    static YFinance.Builder configure(YFinance.Builder builder, GlobalOptions options) {
        Objects.requireNonNull(builder, "builder");
        if (options.noCache()) {
            builder.cache(Cache.none());
        }
        Duration timeout = options.timeout();
        if (timeout != null) {
            builder.requestTimeout(timeout);
        }
        Integer retries = options.retries();
        if (retries != null) {
            builder.maxRetries(retries);
        }
        return builder;
    }
}
