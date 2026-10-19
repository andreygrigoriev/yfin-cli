package io.github.andreygrigoriev.yfin.cli;

import io.github.andreygrigoriev.yfin.cli.output.Format;
import io.github.andreygrigoriev.yfin.request.Frequency;
import io.github.andreygrigoriev.yfin.request.Interval;
import io.github.andreygrigoriev.yfin.request.LookupType;
import io.github.andreygrigoriev.yfin.request.MarketRegion;
import io.github.andreygrigoriev.yfin.request.NewsTab;
import io.github.andreygrigoriev.yfin.request.Period;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/**
 * Type converters for option and parameter values. Enum values are matched case-insensitively,
 * with {@code -} and {@code _} interchangeable, and a bad value lists the valid ones.
 */
public final class Converters {
    private static final Pattern DURATION = Pattern.compile("(\\d+)(ms|s|m|h)");

    private Converters() {}

    /**
     * Registers every converter on {@code commandLine} and the subcommands it has now.
     *
     * @param commandLine the root command line, with its subcommands already added
     * @return {@code commandLine}
     */
    public static CommandLine register(CommandLine commandLine) {
        return commandLine
                .registerConverter(Format.class, enumConverter("format", Format.values(), Converters::label))
                .registerConverter(Period.class, enumConverter("period", Period.values(), Period::code))
                .registerConverter(Interval.class, enumConverter("interval", Interval.values(), Interval::code))
                .registerConverter(Frequency.class, enumConverter("frequency", Frequency.values(), Converters::label))
                .registerConverter(
                        MarketRegion.class, enumConverter("market", MarketRegion.values(), Converters::label))
                .registerConverter(NewsTab.class, enumConverter("news tab", NewsTab.values(), Converters::label))
                .registerConverter(
                        LookupType.class, enumConverter("lookup type", LookupType.values(), Converters::label))
                .registerConverter(LocalDate.class, Converters::date)
                .registerConverter(Duration.class, Converters::duration);
    }

    /**
     * A converter that accepts the label of one of {@code values}.
     *
     * @param what what the value is, for the error message
     * @param values the accepted constants
     * @param label the command-line spelling of a constant
     * @param <E> enum type
     * @return the converter
     */
    static <E extends Enum<E>> ITypeConverter<E> enumConverter(String what, E[] values, Function<E, String> label) {
        List<E> constants = List.of(values);
        return text -> {
            String key = normalize(text);
            for (E e : constants) {
                if (normalize(label.apply(e)).equals(key)) {
                    return e;
                }
            }
            throw new TypeConversionException("'" + text + "' is not a valid " + what + "; valid values: "
                    + constants.stream().map(label).collect(Collectors.joining(", ")));
        };
    }

    /**
     * The command-line spelling of an enum constant: its name in lower case with {@code -} for
     * {@code _}, e.g. {@code press-releases}.
     *
     * @param e the constant
     * @return the label
     */
    static String label(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Parses an ISO date.
     *
     * @param text e.g. {@code 2026-10-19}
     * @return the date
     * @throws TypeConversionException for anything else
     */
    static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new TypeConversionException("'" + text + "' is not a valid date; expected YYYY-MM-DD");
        }
    }

    /**
     * Parses a positive duration: a whole number with a unit {@code ms}, {@code s}, {@code m} or
     * {@code h}, e.g. {@code 30s}, {@code 2m}.
     *
     * @param text the duration
     * @return the duration
     * @throws TypeConversionException for a malformed or zero duration
     */
    static Duration duration(String text) {
        Matcher m = DURATION.matcher(text.strip().toLowerCase(Locale.ROOT));
        if (m.matches()) {
            try {
                long n = Long.parseLong(m.group(1));
                Duration d = switch (m.group(2)) {
                    case "ms" -> Duration.ofMillis(n);
                    case "s" -> Duration.ofSeconds(n);
                    case "m" -> Duration.ofMinutes(n);
                    default -> Duration.ofHours(n);
                };
                if (!d.isZero()) {
                    return d;
                }
            } catch (ArithmeticException | NumberFormatException e) {
                // too large: reported below
            }
        }
        throw new TypeConversionException("'" + text
                + "' is not a valid duration; expected a positive number with a unit, e.g. 500ms, 30s, 2m, 1h");
    }

    /** Converts a whole number that must not be negative, such as {@code --retries}. */
    static final class NonNegativeInt implements ITypeConverter<Integer> {
        @Override
        public Integer convert(String text) {
            try {
                int n = Integer.parseInt(text.strip());
                if (n >= 0) {
                    return n;
                }
            } catch (NumberFormatException e) {
                // reported below
            }
            throw new TypeConversionException("'" + text + "' is not a valid count; expected a whole number >= 0");
        }
    }
}
