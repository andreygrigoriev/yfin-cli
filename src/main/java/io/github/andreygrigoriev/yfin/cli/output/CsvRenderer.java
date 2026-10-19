package io.github.andreygrigoriev.yfin.cli.output;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Renders CSV with RFC 4180 quoting and raw values ({@link Values#raw}). Lines end with
 * {@code \n}. Lists get a header row of column names; records are {@code field,value} pairs.
 */
final class CsvRenderer {
    private CsvRenderer() {}

    /**
     * Prints the header row and one row per element; only the header for an empty list.
     *
     * @param spec the columns
     * @param rows the rows
     * @param out where to print
     * @param <T> row type
     */
    static <T> void list(Spec<T> spec, List<? extends T> rows, PrintWriter out) {
        List<Column<T>> columns = spec.columns();
        line(out, spec.names());
        List<String> fields = new ArrayList<>(columns.size());
        for (T row : rows) {
            fields.clear();
            for (Column<T> column : columns) {
                fields.add(Values.raw(column.kind(), column.valueOf(row)));
            }
            line(out, fields);
        }
        out.flush();
    }

    /**
     * Prints a {@code field,value} header and one line per column; only the header for a
     * {@code null} record.
     *
     * @param spec the columns
     * @param value the record, {@code null} if there is none
     * @param out where to print
     * @param <T> record type
     */
    static <T> void record(Spec<T> spec, @Nullable T value, PrintWriter out) {
        line(out, List.of("field", "value"));
        if (value != null) {
            for (Column<T> column : spec.columns()) {
                line(out, List.of(column.name(), Values.raw(column.kind(), column.valueOf(value))));
            }
        }
        out.flush();
    }

    /**
     * Quotes a field if it contains a comma, a double quote, CR or LF; quotes inside are doubled.
     *
     * @param text the field
     * @return the field as it appears in CSV
     */
    static String quote(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ',' || c == '"' || c == '\r' || c == '\n') {
                return '"' + text.replace("\"", "\"\"") + '"';
            }
        }
        return text;
    }

    private static void line(PrintWriter out, List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(quote(fields.get(i)));
        }
        out.print(line.append('\n'));
    }
}
