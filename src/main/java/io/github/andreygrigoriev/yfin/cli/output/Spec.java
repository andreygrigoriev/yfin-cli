package io.github.andreygrigoriev.yfin.cli.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * An immutable, ordered set of columns describing how to print one row type. Most specs are Java
 * constants built with {@link #builder()}; {@link #dynamic(List)} covers data whose columns are
 * only known at runtime (statement periods, valuation dates).
 *
 * @param <T> row type
 */
public final class Spec<T> {
    /** Name of the leading column added by {@link #withSymbol()}. */
    public static final String SYMBOL = "symbol";

    private final List<Column<T>> columns;

    private Spec(List<Column<T>> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("a spec needs at least one column");
        }
        Set<String> names = new HashSet<>();
        for (Column<T> column : columns) {
            if (!names.add(column.name())) {
                throw new IllegalArgumentException("duplicate column name: '" + column.name() + "'");
            }
        }
        this.columns = List.copyOf(columns);
    }

    /**
     * Starts a spec built column by column.
     *
     * @param <T> row type
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builds a spec from columns assembled at runtime.
     *
     * @param columns the columns, in output order
     * @param <T> row type
     * @return the spec
     * @throws IllegalArgumentException if there are no columns or two share a name
     */
    public static <T> Spec<T> dynamic(List<Column<T>> columns) {
        return new Spec<>(columns);
    }

    /**
     * Returns a spec for symbol-tagged rows: a leading {@value #SYMBOL} text column followed by
     * this spec's columns, read from {@link Symbol#value()}.
     *
     * @return the derived spec
     * @throws IllegalArgumentException if this spec already has a {@value #SYMBOL} column
     */
    public Spec<Symbol<T>> withSymbol() {
        List<Column<Symbol<T>>> derived = new ArrayList<>(columns.size() + 1);
        derived.add(new Column<>(SYMBOL, Symbol::symbol, Kind.TEXT));
        for (Column<T> column : columns) {
            derived.add(new Column<>(column.name(), s -> column.valueOf(s.value()), column.kind()));
        }
        return new Spec<>(derived);
    }

    /**
     * Returns the columns in output order.
     *
     * @return an unmodifiable list
     */
    public List<Column<T>> columns() {
        return columns;
    }

    /**
     * Returns the column names in output order.
     *
     * @return an unmodifiable list
     */
    public List<String> names() {
        return columns.stream().map(Column::name).toList();
    }

    /**
     * Collects columns for {@link Spec#builder()}.
     *
     * @param <T> row type
     */
    public static final class Builder<T> {
        private final List<Column<T>> columns = new ArrayList<>();

        private Builder() {}

        /**
         * Appends a column.
         *
         * @param name snake_case column name
         * @param value reads the value from a row
         * @param kind how the value is formatted
         * @return this builder
         */
        public Builder<T> col(String name, Function<? super T, ? extends @Nullable Object> value, Kind kind) {
            columns.add(new Column<>(name, value, kind));
            return this;
        }

        /**
         * Builds the spec.
         *
         * @return the spec
         * @throws IllegalArgumentException if there are no columns or two share a name
         */
        public Spec<T> build() {
            return new Spec<>(columns);
        }
    }
}
