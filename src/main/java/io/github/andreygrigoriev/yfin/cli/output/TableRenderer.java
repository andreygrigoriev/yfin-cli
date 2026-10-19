package io.github.andreygrigoriev.yfin.cli.output;

import java.io.PrintWriter;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Renders aligned text tables. Numbers are right-aligned, everything else left-aligned, and
 * columns are separated by two spaces. With {@link Style#truncate()}, TEXT cells are shortened
 * with {@value #ELLIPSIS} until the row fits the width; with {@link Style#color()}, headers are
 * bold and positive/negative values in columns named {@code change*} are green/red.
 */
final class TableRenderer {
    static final String ELLIPSIS = "…";
    static final String GAP = "  ";

    /** TEXT columns are never truncated below this width (or their header, if wider). */
    static final int MIN_TEXT_WIDTH = 6;

    private static final String BOLD = "\u001b[1m";
    private static final String GREEN = "\u001b[32m";
    private static final String RED = "\u001b[31m";
    private static final String RESET = "\u001b[0m";

    private final Style style;

    TableRenderer(Style style) {
        this.style = style;
    }

    /**
     * Prints a header row and one row per element. Prints nothing for an empty list.
     *
     * @param spec the columns
     * @param rows the rows
     * @param out where to print
     * @param <T> row type
     */
    <T> void list(Spec<T> spec, List<? extends T> rows, PrintWriter out) {
        if (rows.isEmpty()) {
            return;
        }
        List<Column<T>> columns = spec.columns();
        int n = columns.size();
        @Nullable Object[][] values = new Object[rows.size()][n];
        String[][] cells = new String[rows.size()][n];
        int[] widths = new int[n];
        for (int c = 0; c < n; c++) {
            widths[c] = length(columns.get(c).name());
        }
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < n; c++) {
                Column<T> column = columns.get(c);
                values[r][c] = column.valueOf(rows.get(r));
                cells[r][c] = Values.table(column.kind(), values[r][c]);
                widths[c] = Math.max(widths[c], length(cells[r][c]));
            }
        }
        if (style.truncate()) {
            fit(columns, widths);
        }
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < n; c++) {
            Column<T> column = columns.get(c);
            appendCell(
                    line, column.name(), widths[c], column.kind().numeric(), c == n - 1, style.color() ? BOLD : null);
        }
        out.println(line);
        for (int r = 0; r < rows.size(); r++) {
            line.setLength(0);
            for (int c = 0; c < n; c++) {
                Column<T> column = columns.get(c);
                appendCell(
                        line,
                        truncate(cells[r][c], widths[c]),
                        widths[c],
                        column.kind().numeric(),
                        c == n - 1,
                        colour(column, values[r][c]));
            }
            out.println(line);
        }
        out.flush();
    }

    /**
     * Prints one {@code name  value} line per column, names left-aligned. Prints nothing for a
     * {@code null} record.
     *
     * @param spec the columns
     * @param value the record, {@code null} if there is none
     * @param out where to print
     * @param <T> record type
     */
    <T> void record(Spec<T> spec, @Nullable T value, PrintWriter out) {
        if (value == null) {
            return;
        }
        int keyWidth = 0;
        for (String name : spec.names()) {
            keyWidth = Math.max(keyWidth, length(name));
        }
        int valueWidth = Math.max(MIN_TEXT_WIDTH, style.width() - keyWidth - GAP.length());
        StringBuilder line = new StringBuilder();
        for (Column<T> column : spec.columns()) {
            @Nullable Object raw = column.valueOf(value);
            String cell = Values.table(column.kind(), raw);
            if (style.truncate() && column.kind() == Kind.TEXT) {
                cell = truncate(cell, valueWidth);
            }
            line.setLength(0);
            appendCell(line, column.name(), keyWidth, false, false, null);
            appendCell(line, cell, length(cell), false, true, colour(column, raw));
            out.println(line);
        }
        out.flush();
    }

    /** Narrows the widest TEXT column one step at a time until the row fits or nothing can shrink. */
    private <T> void fit(List<Column<T>> columns, int[] widths) {
        int total = GAP.length() * (widths.length - 1);
        for (int w : widths) {
            total += w;
        }
        while (total > style.width()) {
            int widest = -1;
            for (int c = 0; c < widths.length; c++) {
                Column<T> column = columns.get(c);
                int min = Math.max(MIN_TEXT_WIDTH, length(column.name()));
                if (column.kind() == Kind.TEXT && widths[c] > min && (widest < 0 || widths[c] > widths[widest])) {
                    widest = c;
                }
            }
            if (widest < 0) {
                return;
            }
            widths[widest]--;
            total--;
        }
    }

    private @Nullable String colour(Column<?> column, @Nullable Object value) {
        if (!style.color() || !column.name().startsWith("change") || Values.isMissing(value)) {
            return null;
        }
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return d > 0 ? GREEN : d < 0 ? RED : null;
        }
        return null;
    }

    private static void appendCell(
            StringBuilder line, String text, int width, boolean right, boolean last, @Nullable String ansi) {
        if (!line.isEmpty()) {
            line.append(GAP);
        }
        String pad = " ".repeat(Math.max(0, width - length(text)));
        if (right) {
            line.append(pad);
        }
        if (ansi != null) {
            line.append(ansi).append(text).append(RESET);
        } else {
            line.append(text);
        }
        if (!right && !last) {
            line.append(pad);
        }
    }

    private static String truncate(String text, int width) {
        int length = length(text);
        if (length <= width) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, width - 1)) + ELLIPSIS;
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
