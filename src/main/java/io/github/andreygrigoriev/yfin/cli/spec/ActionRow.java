package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.model.Actions;
import io.github.andreygrigoriev.yfin.model.CapitalGain;
import io.github.andreygrigoriev.yfin.model.Dividend;
import io.github.andreygrigoriev.yfin.model.Split;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * One day of {@link Actions}, like a row of yfinance's {@code actions} frame: the dividend, the
 * split ratio and the capital gain paid that day, each {@code null} when there was none.
 *
 * @param date the event day in the exchange zone
 * @param dividend the dividend per share
 * @param split the split ratio ({@code 4.0} for a 4:1 split)
 * @param capitalGain the capital gain distribution per share
 */
public record ActionRow(
        LocalDate date,
        @Nullable Double dividend,
        @Nullable Double split,
        @Nullable Double capitalGain) {
    /**
     * Validates the date.
     *
     * @throws NullPointerException if {@code date} is {@code null}
     */
    public ActionRow {
        Objects.requireNonNull(date, "date");
    }

    /**
     * Merges the events by day, oldest first. Two events of one kind on one day add up.
     *
     * @param actions the events
     * @return one row per day with an event
     */
    public static List<ActionRow> of(Actions actions) {
        Map<LocalDate, ActionRow> rows = new TreeMap<>();
        for (Dividend d : actions.dividends()) {
            LocalDate day = d.time().toLocalDate();
            ActionRow row = rows.getOrDefault(day, new ActionRow(day, null, null, null));
            rows.put(day, new ActionRow(day, sum(row.dividend, d.amount()), row.split, row.capitalGain));
        }
        for (Split s : actions.splits()) {
            LocalDate day = s.time().toLocalDate();
            ActionRow row = rows.getOrDefault(day, new ActionRow(day, null, null, null));
            rows.put(day, new ActionRow(day, row.dividend, s.ratio(), row.capitalGain));
        }
        for (CapitalGain g : actions.capitalGains()) {
            LocalDate day = g.time().toLocalDate();
            ActionRow row = rows.getOrDefault(day, new ActionRow(day, null, null, null));
            rows.put(day, new ActionRow(day, row.dividend, row.split, sum(row.capitalGain, g.amount())));
        }
        return List.copyOf(new ArrayList<>(rows.values()));
    }

    private static Double sum(@Nullable Double a, double b) {
        return a == null ? b : a + b;
    }
}
