package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.FastInfo;
import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.cli.output.Spec;
import io.github.andreygrigoriev.yfin.model.Bar;
import io.github.andreygrigoriev.yfin.request.Interval;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Column specs of the library models the commands print. */
public final class Specs {
    /** Prices: two fraction digits in tables, full precision in CSV and JSON. */
    public static final Kind PRICE = Kind.decimal(2);

    /**
     * A {@link FastInfo} snapshot, as {@code quote} prints it. {@code change} and {@code change_pct}
     * compare the last price with the previous regular-session close.
     */
    public static final Spec<FastInfo> FAST_INFO = Spec.<FastInfo>builder()
            .col("last_price", FastInfo::lastPrice, PRICE)
            .col("change", Specs::change, PRICE)
            .col("change_pct", Specs::changePct, Kind.PERCENT)
            .col("previous_close", FastInfo::regularMarketPreviousClose, PRICE)
            .col("open", FastInfo::open, PRICE)
            .col("day_high", FastInfo::dayHigh, PRICE)
            .col("day_low", FastInfo::dayLow, PRICE)
            .col("volume", FastInfo::lastVolume, Kind.INTEGER)
            .col("market_cap", FastInfo::marketCap, Kind.INTEGER)
            .col("year_high", FastInfo::yearHigh, PRICE)
            .col("year_low", FastInfo::yearLow, PRICE)
            .col("currency", FastInfo::currency, Kind.TEXT)
            .col("exchange", FastInfo::exchange, Kind.TEXT)
            .build();

    /** Intraday price bars: the bar start as a timestamp in the exchange zone. */
    public static final Spec<Bar> BAR = bars("time", Bar::time, Kind.TIMESTAMP);

    /** Daily and longer price bars: the bar's date in the exchange zone. */
    public static final Spec<Bar> DAILY_BAR = bars("date", b -> b.time().toLocalDate(), Kind.DATE);

    private Specs() {}

    /**
     * The bar spec for an interval: {@link #BAR} for intraday intervals, {@link #DAILY_BAR} otherwise.
     *
     * @param interval the bar size
     * @return the spec
     */
    public static Spec<Bar> bars(Interval interval) {
        return interval.isIntraday() ? BAR : DAILY_BAR;
    }

    private static Spec<Bar> bars(String timeName, Function<Bar, Object> time, Kind timeKind) {
        return Spec.<Bar>builder()
                .col(timeName, time, timeKind)
                .col("open", Bar::open, PRICE)
                .col("high", Bar::high, PRICE)
                .col("low", Bar::low, PRICE)
                .col("close", Bar::close, PRICE)
                .col("adj_close", Bar::adjClose, PRICE)
                .col("volume", Bar::volume, Kind.INTEGER)
                .build();
    }

    private static @Nullable Double change(FastInfo info) {
        Double last = info.lastPrice();
        Double previous = info.regularMarketPreviousClose();
        return last == null || previous == null ? null : last - previous;
    }

    private static @Nullable Double changePct(FastInfo info) {
        Double change = change(info);
        Double previous = info.regularMarketPreviousClose();
        return change == null || previous == null || previous == 0 ? null : change / previous;
    }
}
