package io.github.andreygrigoriev.yfin.cli.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecTest {
    record Row(String name, double price, long volume) {}

    private static final Spec<Row> SPEC = Spec.<Row>builder()
            .col("name", Row::name, Kind.TEXT)
            .col("price", Row::price, Kind.decimal(2))
            .col("volume", Row::volume, Kind.INTEGER)
            .build();

    @Test
    void builderKeepsColumnOrderAndKinds() {
        assertThat(SPEC.names()).containsExactly("name", "price", "volume");
        assertThat(SPEC.columns()).extracting(Column::kind).containsExactly(Kind.TEXT, Kind.decimal(2), Kind.INTEGER);
        Row row = new Row("Apple", 1.5, 7);
        assertThat(SPEC.columns()).extracting(c -> c.valueOf(row)).containsExactly("Apple", 1.5, 7L);
    }

    @Test
    void columnsAreUnmodifiable() {
        assertThatThrownBy(() -> SPEC.columns().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void dynamicCopiesTheList() {
        List<Column<Row>> cols = new ArrayList<>();
        cols.add(new Column<>("field", Row::name, Kind.TEXT));
        cols.add(new Column<>("2025_12_31", Row::price, Kind.decimal(0)));
        Spec<Row> spec = Spec.dynamic(cols);
        cols.clear();
        assertThat(spec.names()).containsExactly("field", "2025_12_31");
    }

    @Test
    void rejectsDuplicateNames() {
        assertThatThrownBy(() -> Spec.<Row>builder()
                        .col("name", Row::name, Kind.TEXT)
                        .col("name", Row::name, Kind.TEXT)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate column name: 'name'");
    }

    @Test
    void rejectsEmptySpec() {
        assertThatThrownBy(() -> Spec.<Row>builder().build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Spec.<Row>dynamic(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonSnakeCaseNames() {
        for (String bad : List.of("", "Name", "lastPrice", "last-price", "_x", "x_", "a__b", "a b", "é")) {
            assertThatThrownBy(() -> new Column<Row>(bad, Row::name, Kind.TEXT))
                    .as(bad)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("snake_case");
        }
        for (String good : List.of("a", "last_price", "eps_q1", "x2_y3", "2025_12_31", "1y")) {
            assertThat(new Column<Row>(good, Row::name, Kind.TEXT).name()).isEqualTo(good);
        }
    }

    @Test
    void rejectsBadDecimalScale() {
        assertThatThrownBy(() -> Kind.decimal(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Kind.decimal(11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withSymbolPrependsSymbolColumn() {
        Spec<Symbol<Row>> spec = SPEC.withSymbol();
        assertThat(spec.names()).containsExactly("symbol", "name", "price", "volume");
        assertThat(spec.columns().getFirst().kind()).isEqualTo(Kind.TEXT);
        Symbol<Row> row = new Symbol<>("AAPL", new Row("Apple", 1.5, 7));
        assertThat(spec.columns()).extracting(c -> c.valueOf(row)).containsExactly("AAPL", "Apple", 1.5, 7L);
    }

    @Test
    void withSymbolRejectsExistingSymbolColumn() {
        Spec<Row> spec = Spec.<Row>builder().col("symbol", Row::name, Kind.TEXT).build();
        assertThatThrownBy(spec::withSymbol)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate column name: 'symbol'");
    }
}
