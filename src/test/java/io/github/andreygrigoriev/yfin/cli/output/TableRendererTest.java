package io.github.andreygrigoriev.yfin.cli.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class TableRendererTest {
    record Row(String name, double price, long volume, double change, LocalDate date) {}

    private static final Spec<Row> SPEC = Spec.<Row>builder()
            .col("name", Row::name, Kind.TEXT)
            .col("price", Row::price, Kind.decimal(2))
            .col("volume", Row::volume, Kind.INTEGER)
            .col("change_pct", Row::change, Kind.PERCENT)
            .col("date", Row::date, Kind.DATE)
            .build();

    private static final List<Row> ROWS = List.of(
            new Row("Apple Inc.", 252.29, 48_123_456, 0.0123, LocalDate.of(2026, 10, 16)),
            new Row("Microsoft Corporation", Double.NaN, 9_000, -0.005, LocalDate.of(2026, 10, 17)));

    private static final Style PLAIN = new Style(false, false, 120);

    @Test
    void alignsTextLeftAndNumbersRight() {
        assertThat(list(PLAIN, SPEC, ROWS)).isEqualTo("""
                        name                    price  volume  change_pct  date
                        Apple Inc.             252.29  48.12M       1.23%  2026-10-16
                        Microsoft Corporation       —    9000      -0.50%  2026-10-17
                        """);
    }

    @Test
    void missingValuesAreDashesInEveryKind() {
        record Empty(String a) {}
        Spec<Empty> spec = Spec.<Empty>builder()
                .col("text", e -> null, Kind.TEXT)
                .col("int", e -> null, Kind.INTEGER)
                .col("dec", e -> Double.NaN, Kind.decimal(2))
                .col("pct", e -> Double.NaN, Kind.PERCENT)
                .col("day", e -> null, Kind.DATE)
                .col("ts", e -> null, Kind.TIMESTAMP)
                .build();
        assertThat(list(PLAIN, spec, List.of(new Empty("x")))).isEqualTo("""
                        text  int  dec  pct  day  ts
                        —       —    —    —  —    —
                        """);
    }

    @Test
    void emptyListPrintsNothing() {
        assertThat(list(PLAIN, SPEC, List.of())).isEmpty();
    }

    @Test
    void noTruncationOffTty() {
        Style narrow = new Style(false, false, 20);
        assertThat(list(narrow, SPEC, ROWS)).contains("Microsoft Corporation");
    }

    @Test
    void truncatesWidestTextColumnToFitWidth() {
        Style narrow = new Style(false, true, 50);
        String out = list(narrow, SPEC, ROWS);
        assertThat(out).isEqualTo("""
                        name         price  volume  change_pct  date
                        Apple Inc.  252.29  48.12M       1.23%  2026-10-16
                        Microsoft…       —    9000      -0.50%  2026-10-17
                        """);
        assertThat(out.lines()).allSatisfy(line -> assertThat(line.length()).isLessThanOrEqualTo(50));
    }

    @Test
    void truncationStopsAtMinimumWidth() {
        Style tiny = new Style(false, true, 10);
        String out = list(tiny, SPEC, ROWS);
        assertThat(out.lines().skip(2).findFirst().orElseThrow()).startsWith("Micro…  ");
    }

    @Test
    void colourMarksChangeColumnsAndBoldHeader() {
        Style colour = new Style(true, false, 120);
        String out = list(colour, SPEC, ROWS);
        assertThat(out.lines().findFirst().orElseThrow()).startsWith("\u001b[1mname\u001b[0m");
        assertThat(out).contains("\u001b[32m1.23%\u001b[0m").contains("\u001b[31m-0.50%\u001b[0m");
        // only change* columns are coloured
        assertThat(out).doesNotContain("\u001b[32m252.29");
    }

    @Test
    void colourKeepsAlignment() {
        String plain = list(PLAIN, SPEC, ROWS);
        String coloured = list(new Style(true, false, 120), SPEC, ROWS).replaceAll("\u001b\\[\\d+m", "");
        assertThat(coloured).isEqualTo(plain);
    }

    @Test
    void zeroAndMissingChangesAreNotColoured() {
        Spec<Row> spec =
                Spec.<Row>builder().col("change", Row::change, Kind.PERCENT).build();
        Row zero = new Row("a", 1, 1, 0.0, LocalDate.EPOCH);
        Row missing = new Row("b", 1, 1, Double.NaN, LocalDate.EPOCH);
        String out = list(new Style(true, false, 120), spec, List.of(zero, missing));
        assertThat(out.lines().skip(1)).containsExactly(" 0.00%", "     —");
    }

    @Test
    void recordIsKeyValueLines() {
        ZonedDateTime ts = ZonedDateTime.of(2026, 10, 16, 9, 30, 0, 0, ZoneId.of("America/New_York"));
        record Info(String symbol, double marketCap, ZonedDateTime time) {}
        Spec<Info> spec = Spec.<Info>builder()
                .col("symbol", Info::symbol, Kind.TEXT)
                .col("market_cap", Info::marketCap, Kind.decimal(0))
                .col("time", Info::time, Kind.TIMESTAMP)
                .build();
        assertThat(record(PLAIN, spec, new Info("AAPL", 3.75e12, ts))).isEqualTo("""
                        symbol      AAPL
                        market_cap  3.75T
                        time        2026-10-16 09:30
                        """);
    }

    @Test
    void recordTruncatesTextValuesOnlyWhenAsked() {
        record Info(String summary) {}
        Spec<Info> spec =
                Spec.<Info>builder().col("summary", Info::summary, Kind.TEXT).build();
        Info info = new Info("A".repeat(30));
        assertThat(record(new Style(false, true, 20), spec, info)).isEqualTo("summary  AAAAAAAAAA…\n");
        assertThat(record(PLAIN, spec, info)).isEqualTo("summary  " + "A".repeat(30) + "\n");
    }

    @Test
    void nullRecordPrintsNothing() {
        assertThat(record(PLAIN, SPEC, null)).isEmpty();
    }

    private static <T> String list(Style style, Spec<T> spec, List<T> rows) {
        return capture(out -> new TableRenderer(style).list(spec, rows, out));
    }

    private static <T> String record(Style style, Spec<T> spec, T value) {
        return capture(out -> new TableRenderer(style).record(spec, value, out));
    }

    static String capture(Consumer<PrintWriter> body) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        body.accept(out);
        out.flush();
        return sw.toString().replace(System.lineSeparator(), "\n");
    }
}
