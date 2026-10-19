package io.github.andreygrigoriev.yfin.cli.spec;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.model.FinancialStatement;
import io.github.andreygrigoriev.yfin.model.LineItem;
import io.github.andreygrigoriev.yfin.model.ValuationMeasures;
import io.github.andreygrigoriev.yfin.request.Frequency;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StatementSpecsTest {
    private static final LocalDate Y2024 = LocalDate.of(2024, 9, 30);
    private static final LocalDate Y2025 = LocalDate.of(2025, 9, 30);

    @Test
    void statementColumnsAreTheLineItemThenPeriodsNewestFirst() {
        var statement = new FinancialStatement(
                "AAPL",
                Frequency.ANNUAL,
                "USD",
                List.of(Y2024, Y2025),
                Map.of(LineItem.TOTAL_REVENUE, new TreeMap<>(Map.of(Y2025, 416.0))));

        var spec = StatementSpecs.of(statement);
        var rows = StatementSpecs.rows(statement);

        assertThat(spec.names()).containsExactly("line_item", "2025_09_30", "2024_09_30");
        assertThat(spec.columns().get(1).kind()).isEqualTo(Kind.decimal(2));
        assertThat(rows).hasSize(1);
        var row = rows.getFirst();
        assertThat(spec.columns().stream().map(c -> c.valueOf(row))).containsExactly("TotalRevenue", 416.0, Double.NaN);
    }

    @Test
    void emptyStatementHasOnlyTheLineItemColumn() {
        var statement = new FinancialStatement("SPY", Frequency.ANNUAL, null, List.of(), Map.of());

        assertThat(StatementSpecs.of(statement).names()).containsExactly("line_item");
        assertThat(StatementSpecs.rows(statement)).isEmpty();
    }

    @Test
    void valuationColumnsAreTheMeasureCurrentThenPeriods() {
        var measures = new ValuationMeasures(
                "AAPL",
                Frequency.ANNUAL,
                Map.of("PeRatio", 30.5),
                List.of(Y2025),
                Map.of("PeRatio", new TreeMap<>(Map.of(Y2025, 28.0)), "PbRatio", new TreeMap<>()));

        var spec = StatementSpecs.of(measures);

        assertThat(spec.names()).containsExactly("measure", "current", "2025_09_30");
        var byName = StatementSpecs.rows(measures).stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e));
        assertThat(spec.columns().stream().map(c -> c.valueOf(byName.get("PeRatio"))))
                .containsExactly("PeRatio", 30.5, 28.0);
        assertThat(spec.columns().stream().map(c -> c.valueOf(byName.get("PbRatio"))))
                .containsExactly("PbRatio", null, Double.NaN);
    }

    @Test
    void emptyValuationHasNoRows() {
        var measures = new ValuationMeasures("SPY", Frequency.QUARTERLY, Map.of(), List.of(), Map.of());

        assertThat(StatementSpecs.of(measures).names()).containsExactly("measure", "current");
        assertThat(StatementSpecs.rows(measures)).isEmpty();
    }

    @Test
    void periodColumnIsTheIsoDateWithUnderscores() {
        assertThat(StatementSpecs.periodColumn(LocalDate.of(2026, 1, 2))).isEqualTo("2026_01_02");
    }
}
