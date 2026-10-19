package io.github.andreygrigoriev.yfin.cli.output;

import java.util.Objects;

/**
 * A row tagged with the symbol it belongs to, for commands that take several symbols.
 *
 * @param symbol the ticker symbol
 * @param value the row
 * @param <T> row type
 * @see Spec#withSymbol()
 */
public record Symbol<T>(String symbol, T value) {
    /** Rejects nulls. */
    public Symbol {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(value, "value");
    }
}
