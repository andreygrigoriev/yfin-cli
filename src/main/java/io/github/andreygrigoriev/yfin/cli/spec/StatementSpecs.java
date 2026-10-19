package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.cli.output.Column;
import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.cli.output.Spec;
import io.github.andreygrigoriev.yfin.model.FinancialStatement;
import io.github.andreygrigoriev.yfin.model.LineItem;
import io.github.andreygrigoriev.yfin.model.ValuationMeasures;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Specs built at runtime for period-column data: one row per line item or measure, one column per
 * period end date, newest first, named by its ISO date with underscores ({@code 2025_09_30}).
 */
public final class StatementSpecs {
    /** Statement values and valuation measures: two fraction digits in tables, full precision elsewhere. */
    public static final Kind VALUE = Kind.decimal(2);

    private StatementSpecs() {}

    /**
     * The spec of {@code statement}: {@code line_item} (the Yahoo key, e.g. {@code TotalRevenue}),
     * then one column per period. Print it over {@link #rows(FinancialStatement)}.
     *
     * @param statement the statement
     * @return the spec
     */
    public static Spec<Map.Entry<LineItem, SortedMap<LocalDate, Double>>> of(FinancialStatement statement) {
        List<Column<Map.Entry<LineItem, SortedMap<LocalDate, Double>>>> columns = new ArrayList<>();
        columns.add(new Column<>("line_item", e -> e.getKey().key(), Kind.TEXT));
        for (LocalDate period : statement.periods()) {
            columns.add(new Column<>(periodColumn(period), e -> e.getValue().get(period), VALUE));
        }
        return Spec.dynamic(columns);
    }

    /**
     * The rows of {@code statement}, in Yahoo website order.
     *
     * @param statement the statement
     * @return one entry per line item
     */
    public static List<Map.Entry<LineItem, SortedMap<LocalDate, Double>>> rows(FinancialStatement statement) {
        return List.copyOf(statement.rows().entrySet());
    }

    /**
     * The spec of {@code measures}, like yfinance's valuation frame: {@code measure} (the Yahoo
     * key, e.g. {@code PeRatio}), {@code current} (the latest trailing value), then one column per
     * period. Print it over {@link #rows(ValuationMeasures)}.
     *
     * @param measures the valuation measures
     * @return the spec
     */
    public static Spec<Map.Entry<String, SortedMap<LocalDate, Double>>> of(ValuationMeasures measures) {
        List<Column<Map.Entry<String, SortedMap<LocalDate, Double>>>> columns = new ArrayList<>();
        columns.add(new Column<>("measure", Map.Entry::getKey, Kind.TEXT));
        columns.add(new Column<>("current", e -> measures.current().get(e.getKey()), VALUE));
        for (LocalDate period : measures.periods()) {
            columns.add(new Column<>(periodColumn(period), e -> e.getValue().get(period), VALUE));
        }
        return Spec.dynamic(columns);
    }

    /**
     * The rows of {@code measures}, in {@link ValuationMeasures#MEASURES} order; none when the
     * result is empty.
     *
     * @param measures the valuation measures
     * @return one entry per measure
     */
    public static List<Map.Entry<String, SortedMap<LocalDate, Double>>> rows(ValuationMeasures measures) {
        return measures.isEmpty() ? List.of() : List.copyOf(measures.rows().entrySet());
    }

    /**
     * The column name of a period: its ISO date with underscores, e.g. {@code 2025_09_30}.
     *
     * @param period the period end date
     * @return the column name
     */
    public static String periodColumn(LocalDate period) {
        return period.toString().replace('-', '_');
    }
}
