package io.github.andreygrigoriev.yfin.cli.output;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Prints command results in the chosen {@link Format}. Data goes to {@code out}; the only thing
 * written to {@code err} is the {@value #NO_DATA} notice for an empty table on a TTY.
 *
 * <p>Empty results are not errors: a table prints nothing, CSV prints only its header, JSON prints
 * {@code []} or {@code {}}.
 */
public final class Output {
    /** Printed to stderr for an empty table result on a TTY. */
    public static final String NO_DATA = "no data";

    private final Format format;
    private final Terminal terminal;
    private final TableRenderer table;
    private final PrintWriter out;
    private final PrintWriter err;

    /**
     * Creates an output.
     *
     * @param format the output format
     * @param terminal where stdout goes; it drives table colour and truncation
     * @param noColor whether {@code --no-color} was given
     * @param out stdout
     * @param err stderr
     */
    public Output(Format format, Terminal terminal, boolean noColor, PrintWriter out, PrintWriter err) {
        this.format = Objects.requireNonNull(format, "format");
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.table = new TableRenderer(Style.of(terminal, noColor));
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    /**
     * Prints a list of rows.
     *
     * @param spec the columns
     * @param rows the rows, possibly empty
     * @param <T> row type
     */
    public <T> void list(Spec<T> spec, List<? extends T> rows) {
        switch (format) {
            case TABLE -> {
                table.list(spec, rows, out);
                if (rows.isEmpty()) {
                    noData();
                }
            }
            case CSV -> CsvRenderer.list(spec, rows, out);
            case JSON -> JsonRenderer.list(spec, rows, out);
        }
    }

    /**
     * Prints a single record.
     *
     * @param spec the fields
     * @param value the record, {@code null} if there is none
     * @param <T> record type
     */
    public <T> void record(Spec<T> spec, @Nullable T value) {
        switch (format) {
            case TABLE -> {
                table.record(spec, value, out);
                if (value == null) {
                    noData();
                }
            }
            case CSV -> CsvRenderer.record(spec, value, out);
            case JSON -> JsonRenderer.record(spec, value, out);
        }
    }

    /**
     * Prints several lists. A table prints each non-empty list, with a blank line between them;
     * CSV prints each list (header and rows), with a blank line between them; JSON prints one
     * object keyed by section name, each value an array.
     *
     * @param sections the lists, in output order
     */
    public void lists(List<Section<?>> sections) {
        switch (format) {
            case TABLE -> {
                boolean printed = false;
                for (Section<?> section : sections) {
                    if (section.rows().isEmpty()) {
                        continue;
                    }
                    if (printed) {
                        out.println();
                    }
                    tableList(section);
                    printed = true;
                }
                out.flush();
                if (!printed) {
                    noData();
                }
            }
            case CSV -> {
                for (int i = 0; i < sections.size(); i++) {
                    if (i > 0) {
                        out.println();
                    }
                    csvList(sections.get(i));
                }
            }
            case JSON -> JsonRenderer.lists(sections, out);
        }
    }

    private <T> void tableList(Section<T> section) {
        table.list(section.spec(), section.rows(), out);
    }

    private <T> void csvList(Section<T> section) {
        CsvRenderer.list(section.spec(), section.rows(), out);
    }

    private void noData() {
        if (terminal.tty()) {
            err.println(NO_DATA);
            err.flush();
        }
    }
}
