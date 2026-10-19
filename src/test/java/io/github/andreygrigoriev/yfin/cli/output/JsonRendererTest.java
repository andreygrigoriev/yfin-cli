package io.github.andreygrigoriev.yfin.cli.output;

import static io.github.andreygrigoriev.yfin.cli.output.TableRendererTest.capture;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonRendererTest {
    record Row(String name, double price, long volume, double change, LocalDate date, ZonedDateTime time) {}

    private static final ZonedDateTime OPEN =
            ZonedDateTime.of(2026, 10, 16, 9, 30, 0, 0, ZoneId.of("America/New_York"));

    private static final Spec<Row> SPEC = Spec.<Row>builder()
            .col("name", Row::name, Kind.TEXT)
            .col("price", Row::price, Kind.decimal(2))
            .col("volume", Row::volume, Kind.INTEGER)
            .col("change", Row::change, Kind.PERCENT)
            .col("date", Row::date, Kind.DATE)
            .col("time", Row::time, Kind.TIMESTAMP)
            .build();

    private static final Row APPLE =
            new Row("Apple \"Inc\"", 252.29, 48_123_456, 0.0123, LocalDate.of(2026, 10, 16), OPEN);

    @Test
    void listIsArrayOfTypedObjects() {
        Row missing = new Row("M", Double.NaN, 0, Double.NaN, LocalDate.of(2026, 10, 17), OPEN);
        assertThat(capture(o -> JsonRenderer.list(SPEC, List.of(APPLE, missing), o)))
                .isEqualTo("""
                        [
                          {
                            "name": "Apple \\"Inc\\"",
                            "price": 252.29,
                            "volume": 48123456,
                            "change": 0.0123,
                            "date": "2026-10-16",
                            "time": "2026-10-16T09:30:00-04:00"
                          },
                          {
                            "name": "M",
                            "price": null,
                            "volume": 0,
                            "change": null,
                            "date": "2026-10-17",
                            "time": "2026-10-16T09:30:00-04:00"
                          }
                        ]
                        """);
    }

    @Test
    void recordIsObject() {
        assertThat(capture(o -> JsonRenderer.record(SPEC, APPLE, o)))
                .startsWith("{\n  \"name\": \"Apple \\\"Inc\\\"\",\n  \"price\": 252.29,")
                .endsWith("\"time\": \"2026-10-16T09:30:00-04:00\"\n}\n");
    }

    @Test
    void emptyResults() {
        assertThat(capture(o -> JsonRenderer.list(SPEC, List.of(), o))).isEqualTo("[]\n");
        assertThat(capture(o -> JsonRenderer.record(SPEC, null, o))).isEqualTo("{}\n");
    }

    @Test
    void numbersStayExactAndPlain() {
        record N(Object value) {}
        Spec<N> spec = Spec.<N>builder()
                .col("big", N::value, Kind.decimal(2))
                .col("int", n -> 12_345_678_901L, Kind.INTEGER)
                .col("text", n -> null, Kind.TEXT)
                .build();
        assertThat(capture(o -> JsonRenderer.record(spec, new N(new BigDecimal("1.5E+12")), o)))
                .isEqualTo("{\n  \"big\": 1500000000000,\n  \"int\": 12345678901,\n  \"text\": null\n}\n");
    }

    @Test
    void infinitiesAreStrings() {
        record N(double value) {}
        Spec<N> spec = Spec.<N>builder().col("ratio", N::value, Kind.decimal(2)).build();
        assertThat(capture(o -> JsonRenderer.list(
                        spec, List.of(new N(Double.POSITIVE_INFINITY), new N(Double.NEGATIVE_INFINITY)), o)))
                .contains("\"ratio\": \"Infinity\"")
                .contains("\"ratio\": \"-Infinity\"");
    }
}
