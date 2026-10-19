package io.github.andreygrigoriev.yfin.cli.output;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One output column: its name, how to read the value from a row, and how to format it.
 *
 * @param name snake_case name (lowercase letters and digits in segments joined by single
 *     underscores; may start with a digit, as period columns like {@code 2025_12_31} do), used as
 *     the JSON key, the CSV header and the table header
 * @param value reads the column value from a row; it may return {@code null} or {@code NaN}
 * @param kind how the value is formatted
 * @param <T> row type
 */
public record Column<T>(String name, Function<? super T, ? extends @Nullable Object> value, Kind kind) {
    private static final Pattern SNAKE_CASE = Pattern.compile("[a-z0-9]+(?:_[a-z0-9]+)*");

    /** Validates the name and rejects nulls. */
    public Column {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(kind, "kind");
        if (!SNAKE_CASE.matcher(name).matches()) {
            throw new IllegalArgumentException("column name must be snake_case: '" + name + "'");
        }
    }

    /**
     * Reads this column's value from a row.
     *
     * @param row the row
     * @return the raw value, possibly {@code null} or {@code NaN}
     */
    public @Nullable Object valueOf(T row) {
        return value.apply(row);
    }
}
