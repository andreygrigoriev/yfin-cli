package io.github.andreygrigoriev.yfin.cli.output;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.Nullable;

/**
 * Formats column values. {@link #table} gives the human form, {@link #raw} the machine form used
 * by CSV and JSON. Missing values ({@code null} or {@code NaN}) are {@value #MISSING} in tables and
 * empty in raw form.
 */
public final class Values {
    /** Table placeholder for a missing value. */
    public static final String MISSING = "—";

    /** INTEGER values from this magnitude up are humanised in tables ({@code 12.35K}). */
    static final double INTEGER_HUMANISE_FROM = 10_000;

    /** DECIMAL values from this magnitude up are humanised in tables ({@code 1.23M}). */
    static final double DECIMAL_HUMANISE_FROM = 1_000_000;

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private static final DateTimeFormatter TABLE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Values() {}

    /**
     * Tells whether a value is missing: {@code null}, or a {@code NaN} double or float.
     *
     * @param value the value
     * @return {@code true} if missing
     */
    public static boolean isMissing(@Nullable Object value) {
        return value == null || (value instanceof Double d && d.isNaN()) || (value instanceof Float f && f.isNaN());
    }

    /**
     * Formats a value for a table: humanised numbers, percentages, short timestamps.
     *
     * @param kind the column kind
     * @param value the value
     * @return the cell text, {@value #MISSING} if missing
     * @throws IllegalArgumentException if the value doesn't fit the kind
     */
    public static String table(Kind kind, @Nullable Object value) {
        if (isMissing(value)) {
            return MISSING;
        }
        return switch (kind) {
            case Kind.Text t -> String.valueOf(value);
            case Kind.Integer i -> {
                double d = number(kind, value).doubleValue();
                if (Double.isInfinite(d)) {
                    yield infinity(d);
                }
                yield Math.abs(d) >= INTEGER_HUMANISE_FROM ? humanise(d) : integer(kind, value);
            }
            case Kind.Decimal(int scale) -> {
                double d = number(kind, value).doubleValue();
                if (Double.isInfinite(d)) {
                    yield infinity(d);
                }
                yield Math.abs(d) >= DECIMAL_HUMANISE_FROM ? humanise(d) : fixed(d, scale);
            }
            case Kind.Percent p -> {
                double d = number(kind, value).doubleValue();
                yield Double.isInfinite(d) ? infinity(d) + "%" : fixed(d * 100, 2) + "%";
            }
            case Kind.Bool b -> bool(value).toString();
            case Kind.Date dt -> date(value).toString();
            case Kind.Timestamp ts -> TABLE_TIMESTAMP.format(timestamp(value));
        };
    }

    /**
     * Formats a value for CSV and JSON: plain numbers (no exponent, no humanising), fractions for
     * percentages, ISO dates and ISO timestamps with offset.
     *
     * @param kind the column kind
     * @param value the value
     * @return the text, empty if missing; infinities are {@code Infinity} and {@code -Infinity}
     * @throws IllegalArgumentException if the value doesn't fit the kind
     */
    public static String raw(Kind kind, @Nullable Object value) {
        if (isMissing(value)) {
            return "";
        }
        return switch (kind) {
            case Kind.Text t -> String.valueOf(value);
            case Kind.Integer i -> {
                double d = number(kind, value).doubleValue();
                yield Double.isInfinite(d) ? String.valueOf(d) : integer(kind, value);
            }
            case Kind.Decimal dec -> plain(number(kind, value));
            case Kind.Percent p -> plain(number(kind, value));
            case Kind.Bool b -> bool(value).toString();
            case Kind.Date dt -> date(value).toString();
            case Kind.Timestamp ts -> {
                ZonedDateTime z = timestamp(value);
                yield value instanceof Instant
                        ? DateTimeFormatter.ISO_INSTANT.format(z)
                        : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(z);
            }
        };
    }

    private static Number number(Kind kind, @Nullable Object value) {
        if (value instanceof Number n) {
            return n;
        }
        throw mismatch(kind, value);
    }

    private static String integer(Kind kind, @Nullable Object value) {
        Number n = number(kind, value);
        return switch (n) {
            case Long l -> l.toString();
            case java.lang.Integer i -> i.toString();
            case Short s -> s.toString();
            case Byte b -> b.toString();
            case BigInteger b -> b.toString();
            case BigDecimal b -> b.setScale(0, RoundingMode.HALF_UP).toPlainString();
            default ->
                BigDecimal.valueOf(n.doubleValue())
                        .setScale(0, RoundingMode.HALF_UP)
                        .toPlainString();
        };
    }

    private static String plain(Number n) {
        BigDecimal b = switch (n) {
            case BigDecimal d -> d;
            case BigInteger i -> new BigDecimal(i);
            case Long l -> BigDecimal.valueOf(l);
            case java.lang.Integer i -> BigDecimal.valueOf(i);
            default -> {
                double d = n.doubleValue();
                if (Double.isInfinite(d)) {
                    yield null;
                }
                yield BigDecimal.valueOf(d);
            }
        };
        if (b == null) {
            return String.valueOf(n.doubleValue());
        }
        b = b.stripTrailingZeros();
        return b.signum() == 0 ? "0" : b.toPlainString();
    }

    private static String fixed(double d, int scale) {
        return BigDecimal.valueOf(d).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    /** {@code 1234567 -> 1.23M}: two decimals, the largest unit that keeps the mantissa below 1000. */
    private static String humanise(double d) {
        BigDecimal abs = BigDecimal.valueOf(Math.abs(d));
        int unit = 0;
        while (unit < SUFFIXES.length - 1 && abs.compareTo(THOUSAND) >= 0) {
            abs = abs.divide(THOUSAND);
            unit++;
        }
        BigDecimal rounded = abs.setScale(2, RoundingMode.HALF_UP);
        // 999_999 rounds to 1000.00K; carry into the next unit instead
        if (rounded.compareTo(THOUSAND) >= 0 && unit < SUFFIXES.length - 1) {
            rounded = rounded.divide(THOUSAND).setScale(2, RoundingMode.HALF_UP);
            unit++;
        }
        return (d < 0 ? "-" : "") + rounded.toPlainString() + SUFFIXES[unit];
    }

    private static String infinity(double d) {
        return d > 0 ? "∞" : "-∞";
    }

    private static Boolean bool(@Nullable Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw mismatch(Kind.BOOLEAN, value);
    }

    private static LocalDate date(@Nullable Object value) {
        return switch (value) {
            case LocalDate d -> d;
            case ZonedDateTime z -> z.toLocalDate();
            case OffsetDateTime o -> o.toLocalDate();
            case LocalDateTime l -> l.toLocalDate();
            case Instant i -> LocalDate.ofInstant(i, ZoneOffset.UTC);
            case null, default -> throw mismatch(Kind.DATE, value);
        };
    }

    private static ZonedDateTime timestamp(@Nullable Object value) {
        return switch (value) {
            case ZonedDateTime z -> z;
            case OffsetDateTime o -> o.toZonedDateTime();
            case Instant i -> i.atZone(ZoneOffset.UTC);
            case null, default -> throw mismatch(Kind.TIMESTAMP, value);
        };
    }

    private static IllegalArgumentException mismatch(Kind kind, @Nullable Object value) {
        String type = value == null ? "null" : value.getClass().getSimpleName();
        return new IllegalArgumentException("value of type " + type + " doesn't fit column kind " + kind);
    }
}
