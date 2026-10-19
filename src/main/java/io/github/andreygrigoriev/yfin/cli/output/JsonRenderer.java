package io.github.andreygrigoriev.yfin.cli.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Renders pretty-printed JSON with the streaming {@link JsonGenerator}: a list is an array of
 * objects, a record is an object, keyed by column name. Numeric kinds are JSON numbers (raw, see
 * {@link Values#raw}; infinities are the strings {@code "Infinity"}/{@code "-Infinity"}), other
 * kinds are strings, and missing values are {@code null}.
 */
final class JsonRenderer {
    private static final JsonFactory FACTORY =
            JsonFactory.builder().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

    private static final DefaultIndenter INDENTER = new DefaultIndenter("  ", "\n");

    private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter(Separators.createDefaultInstance()
                    .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                    .withObjectEmptySeparator("")
                    .withArrayEmptySeparator(""))
            .withObjectIndenter(INDENTER)
            .withArrayIndenter(INDENTER);

    private JsonRenderer() {}

    /**
     * Prints an array with one object per row; {@code []} for an empty list.
     *
     * @param spec the columns
     * @param rows the rows
     * @param out where to print
     * @param <T> row type
     */
    static <T> void list(Spec<T> spec, List<? extends T> rows, PrintWriter out) {
        write(out, g -> {
            g.writeStartArray();
            for (T row : rows) {
                object(g, spec, row);
            }
            g.writeEndArray();
        });
    }

    /**
     * Prints one object; {@code {}} for a {@code null} record.
     *
     * @param spec the columns
     * @param value the record, {@code null} if there is none
     * @param out where to print
     * @param <T> record type
     */
    static <T> void record(Spec<T> spec, @Nullable T value, PrintWriter out) {
        write(out, g -> {
            if (value == null) {
                g.writeStartObject();
                g.writeEndObject();
            } else {
                object(g, spec, value);
            }
        });
    }

    private static <T> void object(JsonGenerator g, Spec<T> spec, T row) throws IOException {
        g.writeStartObject();
        for (Column<T> column : spec.columns()) {
            g.writeFieldName(column.name());
            value(g, column.kind(), column.valueOf(row));
        }
        g.writeEndObject();
    }

    private static void value(JsonGenerator g, Kind kind, @Nullable Object value) throws IOException {
        if (Values.isMissing(value)) {
            g.writeNull();
            return;
        }
        String raw = Values.raw(kind, value);
        if (kind.numeric() && !raw.endsWith("Infinity")) {
            g.writeNumber(raw);
        } else {
            g.writeString(raw);
        }
    }

    private static void write(PrintWriter out, Body body) {
        try (JsonGenerator g = FACTORY.createGenerator(out)) {
            g.setPrettyPrinter(PRINTER.createInstance());
            body.write(g);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.print('\n');
        out.flush();
    }

    @FunctionalInterface
    private interface Body {
        void write(JsonGenerator g) throws IOException;
    }
}
