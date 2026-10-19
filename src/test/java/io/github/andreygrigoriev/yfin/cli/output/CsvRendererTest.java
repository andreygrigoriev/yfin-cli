package io.github.andreygrigoriev.yfin.cli.output;

import static io.github.andreygrigoriev.yfin.cli.output.TableRendererTest.capture;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvRendererTest {
    record Row(String name, double price, long volume, double change, ZonedDateTime time) {}

    private static final ZonedDateTime OPEN =
            ZonedDateTime.of(2026, 10, 16, 9, 30, 0, 0, ZoneId.of("America/New_York"));

    private static final Spec<Row> SPEC = Spec.<Row>builder()
            .col("name", Row::name, Kind.TEXT)
            .col("price", Row::price, Kind.decimal(2))
            .col("volume", Row::volume, Kind.INTEGER)
            .col("change", Row::change, Kind.PERCENT)
            .col("time", Row::time, Kind.TIMESTAMP)
            .build();

    @Test
    void listHasHeaderAndRawValues() {
        String out = capture(out1 -> CsvRenderer.list(
                SPEC,
                List.of(
                        new Row("Apple", 252.29, 48_123_456, 0.0123, OPEN),
                        new Row("Missing", Double.NaN, 0, Double.NaN, OPEN)),
                out1));
        assertThat(out).isEqualTo("""
                        name,price,volume,change,time
                        Apple,252.29,48123456,0.0123,2026-10-16T09:30:00-04:00
                        Missing,,0,,2026-10-16T09:30:00-04:00
                        """);
    }

    @Test
    void missingFirstFieldKeepsSeparators() {
        Spec<Row> spec = Spec.<Row>builder()
                .col("price", Row::price, Kind.decimal(2))
                .col("change", Row::change, Kind.PERCENT)
                .col("name", Row::name, Kind.TEXT)
                .build();
        String out = capture(o -> CsvRenderer.list(spec, List.of(new Row("x", Double.NaN, 0, Double.NaN, OPEN)), o));
        assertThat(out).isEqualTo("price,change,name\n,,x\n");
    }

    @Test
    void emptyListPrintsHeaderOnly() {
        assertThat(capture(o -> CsvRenderer.list(SPEC, List.of(), o))).isEqualTo("name,price,volume,change,time\n");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            quoteCharacter = '\'',
            value = {
                "plain|plain",
                "a,b|'\"a,b\"'",
                "say \"hi\"|'\"say \"\"hi\"\"\"'",
                "'line1\nline2'|'\"line1\nline2\"'",
                "'cr\rhere'|'\"cr\rhere\"'",
                "''|''"
            })
    void quotesPerRfc4180(String input, String expected) {
        assertThat(CsvRenderer.quote(input)).isEqualTo(expected);
    }

    @Test
    void quotedFieldsInRows() {
        String out = capture(o -> CsvRenderer.list(SPEC, List.of(new Row("Apple, \"Inc\"\nUS", 1, 2, 0, OPEN)), o));
        assertThat(out.lines().skip(1).findFirst().orElseThrow()).isEqualTo("\"Apple, \"\"Inc\"\"");
        assertThat(out).endsWith("US\",1,2,0,2026-10-16T09:30:00-04:00\n");
    }

    @Test
    void recordIsFieldValuePairs() {
        String out = capture(o -> CsvRenderer.record(SPEC, new Row("A,B", 1.5, 10, Double.NaN, OPEN), o));
        assertThat(out).isEqualTo("""
                        field,value
                        name,"A,B"
                        price,1.5
                        volume,10
                        change,
                        time,2026-10-16T09:30:00-04:00
                        """);
    }

    @Test
    void nullRecordPrintsHeaderOnly() {
        assertThat(capture(o -> CsvRenderer.record(SPEC, null, o))).isEqualTo("field,value\n");
    }
}
