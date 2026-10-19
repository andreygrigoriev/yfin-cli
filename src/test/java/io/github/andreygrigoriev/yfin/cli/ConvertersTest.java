package io.github.andreygrigoriev.yfin.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.andreygrigoriev.yfin.cli.output.Format;
import io.github.andreygrigoriev.yfin.request.Frequency;
import io.github.andreygrigoriev.yfin.request.Interval;
import io.github.andreygrigoriev.yfin.request.LookupType;
import io.github.andreygrigoriev.yfin.request.MarketRegion;
import io.github.andreygrigoriev.yfin.request.NewsTab;
import io.github.andreygrigoriev.yfin.request.Period;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

class ConvertersTest {

    @ParameterizedTest
    @CsvSource({"1mo, ONE_MONTH", "YTD, YTD", " 5d , FIVE_DAYS", "max, MAX"})
    void period(String text, Period expected) throws Exception {
        assertThat(converter(Period.values(), Period::code).convert(text)).isEqualTo(expected);
    }

    @Test
    void periodErrorListsCodes() {
        assertThatThrownBy(() -> converter(Period.values(), Period::code).convert("3y"))
                .isInstanceOf(TypeConversionException.class)
                .hasMessage("'3y' is not a valid test; valid values: 1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max");
    }

    @ParameterizedTest
    @CsvSource({"1m, ONE_MINUTE", "1WK, ONE_WEEK", "1h, ONE_HOUR", "60m, SIXTY_MINUTES"})
    void interval(String text, Interval expected) throws Exception {
        assertThat(converter(Interval.values(), Interval::code).convert(text)).isEqualTo(expected);
    }

    @Test
    void labelIsLowerCaseKebab() {
        assertThat(Converters.label(NewsTab.PRESS_RELEASES)).isEqualTo("press-releases");
        assertThat(Converters.label(MarketRegion.US)).isEqualTo("us");
    }

    @ParameterizedTest
    @CsvSource({"press-releases, PRESS_RELEASES", "PRESS_RELEASES, PRESS_RELEASES", "News, NEWS"})
    void newsTabAcceptsDashOrUnderscoreInAnyCase(String text, NewsTab expected) throws Exception {
        assertThat(converter(NewsTab.values(), Converters::label).convert(text)).isEqualTo(expected);
    }

    @Test
    void lookupTypeErrorListsLabels() {
        assertThatThrownBy(
                        () -> converter(LookupType.values(), Converters::label).convert("bond"))
                .hasMessage("'bond' is not a valid test; valid values: all, stock, mutual-fund, etf, index, future,"
                        + " currency, cryptocurrency");
    }

    @ParameterizedTest
    @CsvSource({"2026-10-19, 2026-10-19", " 2000-02-29 , 2000-02-29"})
    void date(String text, LocalDate expected) {
        assertThat(Converters.date(text)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"19/10/2026", "2026-02-30", "yesterday"})
    void invalidDate(String text) {
        assertThatThrownBy(() -> Converters.date(text))
                .isInstanceOf(TypeConversionException.class)
                .hasMessage("'" + text + "' is not a valid date; expected YYYY-MM-DD");
    }

    @ParameterizedTest
    @CsvSource({"500ms, PT0.5S", "30s, PT30S", "2m, PT2M", "1H, PT1H", " 90S , PT1M30S"})
    void duration(String text, Duration expected) {
        assertThat(Converters.duration(text)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0s", "30", "-5s", "1d", "PT30S", "1.5s", "99999999999999999999h", "9999999999999999h"})
    void invalidDuration(String text) {
        assertThatThrownBy(() -> Converters.duration(text))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageStartingWith("'" + text + "' is not a valid duration");
    }

    @Test
    void nonNegativeInt() {
        Converters.NonNegativeInt c = new Converters.NonNegativeInt();
        assertThat(c.convert("0")).isZero();
        assertThat(c.convert("5")).isEqualTo(5);
        assertThatThrownBy(() -> c.convert("-1")).hasMessage("'-1' is not a valid count; expected a whole number >= 0");
        assertThatThrownBy(() -> c.convert("x")).isInstanceOf(TypeConversionException.class);
    }

    @Test
    void registeredOnCommandLine() {
        Probe probe = new Probe();
        int code = run(
                probe,
                "--format",
                "JSON",
                "--period",
                "1y",
                "--interval",
                "1wk",
                "--frequency",
                "quarterly",
                "--market",
                "Europe",
                "--tab",
                "all",
                "--type",
                "mutual_fund",
                "--date",
                "2026-10-19",
                "--wait",
                "2m");
        assertThat(code).isZero();
        assertThat(probe.format).isEqualTo(Format.JSON);
        assertThat(probe.period).isEqualTo(Period.ONE_YEAR);
        assertThat(probe.interval).isEqualTo(Interval.ONE_WEEK);
        assertThat(probe.frequency).isEqualTo(Frequency.QUARTERLY);
        assertThat(probe.market).isEqualTo(MarketRegion.EUROPE);
        assertThat(probe.tab).isEqualTo(NewsTab.ALL);
        assertThat(probe.type).isEqualTo(LookupType.MUTUAL_FUND);
        assertThat(probe.date).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(probe.wait).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void conversionErrorIsUsageErrorListingValidValues() {
        StringWriter err = new StringWriter();
        CommandLine cl = Converters.register(new CommandLine(new Probe())).setErr(new PrintWriter(err, true));
        assertThat(cl.execute("--market", "mars")).isEqualTo(2);
        assertThat(err.toString())
                .contains("Invalid value for option '--market'")
                .contains("'mars' is not a valid market; valid values: us, gb, asia, europe, rates, commodities,"
                        + " currencies, cryptocurrencies");
    }

    private static <E extends Enum<E>> ITypeConverter<E> converter(E[] values, Function<E, String> label) {
        return Converters.enumConverter("test", values, label);
    }

    private static int run(Object command, String... args) {
        return Converters.register(new CommandLine(command))
                .setErr(new PrintWriter(new StringWriter(), true))
                .execute(args);
    }

    @Command(name = "probe")
    static final class Probe implements Runnable {
        @Option(names = "--format")
        Format format = Format.TABLE;

        @Option(names = "--period")
        Period period = Period.MAX;

        @Option(names = "--interval")
        Interval interval = Interval.ONE_DAY;

        @Option(names = "--frequency")
        Frequency frequency = Frequency.ANNUAL;

        @Option(names = "--market")
        MarketRegion market = MarketRegion.US;

        @Option(names = "--tab")
        NewsTab tab = NewsTab.NEWS;

        @Option(names = "--type")
        LookupType type = LookupType.ALL;

        @Option(names = "--date")
        LocalDate date = LocalDate.EPOCH;

        @Option(names = "--wait")
        Duration wait = Duration.ZERO;

        @Override
        public void run() {}
    }
}
