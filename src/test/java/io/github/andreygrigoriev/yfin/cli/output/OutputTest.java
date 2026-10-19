package io.github.andreygrigoriev.yfin.cli.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OutputTest {
    record Row(String name, double change) {}

    private static final Spec<Row> SPEC = Spec.<Row>builder()
            .col("name", Row::name, Kind.TEXT)
            .col("change", Row::change, Kind.PERCENT)
            .build();

    private static final Terminal TTY = new Terminal(true, 120, false);
    private static final Terminal PIPE = Terminal.piped(120);

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"TABLE|''", "CSV|'name,change\n'", "JSON|'[]\n'"})
    void emptyListPerFormat(Format format, String expected) {
        output(format, PIPE, false).list(SPEC, List.of());
        assertThat(stdout()).isEqualTo(expected);
        assertThat(err.toString()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"TABLE|''", "CSV|'field,value\n'", "JSON|'{}\n'"})
    void emptyRecordPerFormat(Format format, String expected) {
        output(format, PIPE, false).record(SPEC, null);
        assertThat(stdout()).isEqualTo(expected);
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void emptyTableOnTtySaysNoDataOnStderr() {
        output(Format.TABLE, TTY, false).list(SPEC, List.of());
        assertThat(stdout()).isEmpty();
        assertThat(err.toString().strip()).isEqualTo("no data");

        err.getBuffer().setLength(0);
        output(Format.TABLE, TTY, false).record(SPEC, null);
        assertThat(err.toString().strip()).isEqualTo("no data");
    }

    @Test
    void emptyCsvAndJsonOnTtyStayQuiet() {
        output(Format.CSV, TTY, false).list(SPEC, List.of());
        output(Format.JSON, TTY, false).record(SPEC, null);
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void formatDoesNotDependOnTty() {
        output(Format.JSON, TTY, false).list(SPEC, List.of(new Row("a", 0.1)));
        String onTty = stdout();
        out.getBuffer().setLength(0);
        output(Format.JSON, PIPE, false).list(SPEC, List.of(new Row("a", 0.1)));
        assertThat(stdout()).isEqualTo(onTty).startsWith("[");
    }

    @Test
    void colourOnlyOnTtyWithoutOptOuts() {
        List<Row> rows = List.of(new Row("a", 0.1));
        assertThat(table(TTY, false, rows)).contains("\u001b[");
        assertThat(table(PIPE, false, rows)).doesNotContain("\u001b[");
        assertThat(table(TTY, true, rows)).doesNotContain("\u001b[");
        assertThat(table(new Terminal(true, 120, true), false, rows)).doesNotContain("\u001b[");
    }

    @Test
    void recordInEachFormat() {
        Row row = new Row("a", 0.25);
        output(Format.TABLE, PIPE, false).record(SPEC, row);
        output(Format.CSV, PIPE, false).record(SPEC, row);
        output(Format.JSON, PIPE, false).record(SPEC, row);
        assertThat(stdout()).isEqualTo("""
                        name    a
                        change  25.00%
                        field,value
                        name,a
                        change,0.25
                        {
                          "name": "a",
                          "change": 0.25
                        }
                        """);
    }

    @Test
    void listsInEachFormat() {
        List<Section<?>> sections = List.of(
                new Section<>("first", SPEC, List.of(new Row("a", 0.25))),
                new Section<>("none", SPEC, List.of()),
                new Section<>("second", SPEC, List.of(new Row("b", -0.5))));
        output(Format.TABLE, PIPE, false).lists(sections);
        output(Format.CSV, PIPE, false).lists(sections);
        output(Format.JSON, PIPE, false).lists(sections);
        assertThat(stdout()).isEqualTo("""
                        name  change
                        a     25.00%

                        name   change
                        b     -50.00%
                        name,change
                        a,0.25

                        name,change

                        name,change
                        b,-0.5
                        {
                          "first": [
                            {
                              "name": "a",
                              "change": 0.25
                            }
                          ],
                          "none": [],
                          "second": [
                            {
                              "name": "b",
                              "change": -0.5
                            }
                          ]
                        }
                        """);
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void allEmptyListsOnTtySayNoData() {
        List<Section<?>> sections = List.of(new Section<>("only", SPEC, List.of()));
        output(Format.TABLE, TTY, false).lists(sections);
        assertThat(stdout()).isEmpty();
        assertThat(err.toString().strip()).isEqualTo("no data");
    }

    @Test
    void terminalFromEnvironment() {
        assertThat(Terminal.of(true, Map.of())).isEqualTo(new Terminal(true, 120, false));
        assertThat(Terminal.of(false, Map.of("COLUMNS", "80", "NO_COLOR", "1")))
                .isEqualTo(new Terminal(false, 80, true));
        assertThat(Terminal.of(true, Map.of("COLUMNS", "abc", "NO_COLOR", "")))
                .isEqualTo(new Terminal(true, 120, false));
        assertThat(Terminal.of(true, Map.of("COLUMNS", "-5")))
                .extracting(Terminal::width)
                .isEqualTo(120);
        assertThatThrownBy(() -> new Terminal(true, 0, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void styleFromTerminal() {
        assertThat(Style.of(TTY, false)).isEqualTo(new Style(true, true, 120));
        assertThat(Style.of(TTY, true)).isEqualTo(new Style(false, true, 120));
        assertThat(Style.of(Terminal.piped(80), false)).isEqualTo(new Style(false, false, 80));
    }

    private String table(Terminal terminal, boolean noColor, List<Row> rows) {
        out.getBuffer().setLength(0);
        output(Format.TABLE, terminal, noColor).list(SPEC, rows);
        return stdout();
    }

    private Output output(Format format, Terminal terminal, boolean noColor) {
        return new Output(format, terminal, noColor, new PrintWriter(out, true), new PrintWriter(err, true));
    }

    private String stdout() {
        return out.toString().replace(System.lineSeparator(), "\n");
    }
}
