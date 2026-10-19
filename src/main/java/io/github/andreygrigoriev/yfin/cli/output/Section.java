package io.github.andreygrigoriev.yfin.cli.output;

import java.util.List;
import java.util.Objects;

/**
 * One named list of a result made of several lists, such as {@code search}'s quotes and news.
 *
 * @param name the JSON key of the list
 * @param spec the columns
 * @param rows the rows, possibly empty
 * @param <T> row type
 * @see Output#lists(List)
 */
public record Section<T>(String name, Spec<T> spec, List<? extends T> rows) {
    /** Rejects nulls and copies the rows. */
    public Section {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(spec, "spec");
        rows = List.copyOf(rows);
    }
}
