package io.github.andreygrigoriev.yfin.cli.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValuesTest {
    private static final ZonedDateTime NY_OPEN =
            ZonedDateTime.of(2026, 10, 16, 9, 30, 0, 0, ZoneId.of("America/New_York"));

    @Test
    void missingValues() {
        assertThat(Values.isMissing(null)).isTrue();
        assertThat(Values.isMissing(Double.NaN)).isTrue();
        assertThat(Values.isMissing(Float.NaN)).isTrue();
        assertThat(Values.isMissing(0.0)).isFalse();
        assertThat(Values.isMissing(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(Values.isMissing("")).isFalse();
    }

    @Test
    void missingIsDashInTableAndEmptyRaw() {
        for (Kind kind :
                new Kind[] {Kind.TEXT, Kind.INTEGER, Kind.decimal(2), Kind.PERCENT, Kind.DATE, Kind.TIMESTAMP}) {
            assertThat(Values.table(kind, null)).as("%s", kind).isEqualTo("—");
            assertThat(Values.table(kind, Double.NaN)).as("%s", kind).isEqualTo("—");
            assertThat(Values.raw(kind, null)).as("%s", kind).isEmpty();
            assertThat(Values.raw(kind, Double.NaN)).as("%s", kind).isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "42, 42",
        "-42, -42",
        "9999, 9999",
        "10000, 10.00K",
        "12345, 12.35K",
        "-12345, -12.35K",
        "999999, 1.00M",
        "1234567, 1.23M",
        "1234567890, 1.23B",
        "3500000000000, 3.50T",
        "1234500000000000, 1234.50T",
    })
    void integerTable(long value, String expected) {
        assertThat(Values.table(Kind.INTEGER, value)).isEqualTo(expected);
    }

    @Test
    void integerAcceptsOtherNumberTypes() {
        assertThat(Values.table(Kind.INTEGER, 7)).isEqualTo("7");
        assertThat(Values.table(Kind.INTEGER, 7.0)).isEqualTo("7");
        assertThat(Values.table(Kind.INTEGER, 6.5)).isEqualTo("7");
        assertThat(Values.raw(Kind.INTEGER, 1.5e10)).isEqualTo("15000000000");
        assertThat(Values.raw(Kind.INTEGER, Long.MAX_VALUE)).isEqualTo("9223372036854775807");
        assertThat(Values.raw(Kind.INTEGER, -3)).isEqualTo("-3");
        assertThat(Values.raw(Kind.INTEGER, 0L)).isEqualTo("0");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0.00",
        "-0.0, 0.00",
        "1.005, 1.01",
        "123.456, 123.46",
        "-123.456, -123.46",
        "999999.99, 999999.99",
        "1000000, 1.00M",
        "-2.5e9, -2.50B",
        "1e-7, 0.00",
    })
    void decimalTable(double value, String expected) {
        assertThat(Values.table(Kind.decimal(2), value)).isEqualTo(expected);
    }

    @Test
    void decimalScale() {
        assertThat(Values.table(Kind.decimal(0), 12.5)).isEqualTo("13");
        assertThat(Values.table(Kind.decimal(4), 0.12345)).isEqualTo("0.1235");
    }

    @Test
    void decimalRawIsPlain() {
        assertThat(Values.raw(Kind.decimal(2), 123.456)).isEqualTo("123.456");
        assertThat(Values.raw(Kind.decimal(2), 1.0)).isEqualTo("1");
        assertThat(Values.raw(Kind.decimal(2), 0.0)).isEqualTo("0");
        assertThat(Values.raw(Kind.decimal(2), -0.0)).isEqualTo("0");
        assertThat(Values.raw(Kind.decimal(2), 1.5e15)).isEqualTo("1500000000000000");
        assertThat(Values.raw(Kind.decimal(2), 1e-7)).isEqualTo("0.0000001");
        assertThat(Values.raw(Kind.decimal(2), -42.5)).isEqualTo("-42.5");
        assertThat(Values.raw(Kind.decimal(2), new BigDecimal("1.2300"))).isEqualTo("1.23");
        assertThat(Values.raw(Kind.decimal(2), 5L)).isEqualTo("5");
    }

    @Test
    void infinity() {
        double inf = Double.POSITIVE_INFINITY;
        assertThat(Values.table(Kind.INTEGER, inf)).isEqualTo("∞");
        assertThat(Values.table(Kind.decimal(2), -inf)).isEqualTo("-∞");
        assertThat(Values.table(Kind.PERCENT, inf)).isEqualTo("∞%");
        assertThat(Values.raw(Kind.INTEGER, inf)).isEqualTo("Infinity");
        assertThat(Values.raw(Kind.decimal(2), -inf)).isEqualTo("-Infinity");
        assertThat(Values.raw(Kind.PERCENT, inf)).isEqualTo("Infinity");
    }

    @Test
    void percent() {
        assertThat(Values.table(Kind.PERCENT, 0.1234)).isEqualTo("12.34%");
        assertThat(Values.table(Kind.PERCENT, -0.05)).isEqualTo("-5.00%");
        assertThat(Values.table(Kind.PERCENT, 0.0)).isEqualTo("0.00%");
        assertThat(Values.table(Kind.PERCENT, 12.5)).isEqualTo("1250.00%");
        assertThat(Values.raw(Kind.PERCENT, 0.1234)).isEqualTo("0.1234");
        assertThat(Values.raw(Kind.PERCENT, -0.05)).isEqualTo("-0.05");
    }

    @Test
    void text() {
        assertThat(Values.table(Kind.TEXT, "Apple, Inc.")).isEqualTo("Apple, Inc.");
        assertThat(Values.raw(Kind.TEXT, "")).isEmpty();
        assertThat(Values.raw(Kind.TEXT, 3)).isEqualTo("3");
    }

    @Test
    void dates() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        assertThat(Values.table(Kind.DATE, date)).isEqualTo("2026-10-18");
        assertThat(Values.raw(Kind.DATE, date)).isEqualTo("2026-10-18");
        assertThat(Values.table(Kind.DATE, NY_OPEN)).isEqualTo("2026-10-16");
        assertThat(Values.raw(Kind.DATE, Instant.parse("2026-10-18T23:30:00Z"))).isEqualTo("2026-10-18");
    }

    @Test
    void timestampsKeepTheirZone() {
        assertThat(Values.table(Kind.TIMESTAMP, NY_OPEN)).isEqualTo("2026-10-16 09:30");
        assertThat(Values.raw(Kind.TIMESTAMP, NY_OPEN)).isEqualTo("2026-10-16T09:30:00-04:00");
        assertThat(Values.raw(Kind.TIMESTAMP, NY_OPEN.toOffsetDateTime())).isEqualTo("2026-10-16T09:30:00-04:00");
        Instant instant = Instant.parse("2026-10-16T13:30:00Z");
        assertThat(Values.table(Kind.TIMESTAMP, instant)).isEqualTo("2026-10-16 13:30");
        assertThat(Values.raw(Kind.TIMESTAMP, instant)).isEqualTo("2026-10-16T13:30:00Z");
    }

    @Test
    void kindMismatchFailsLoudly() {
        assertThatThrownBy(() -> Values.table(Kind.INTEGER, "12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String");
        assertThatThrownBy(() -> Values.raw(Kind.DATE, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Values.raw(Kind.TIMESTAMP, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
