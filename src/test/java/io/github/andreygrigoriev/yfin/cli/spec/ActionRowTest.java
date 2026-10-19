package io.github.andreygrigoriev.yfin.cli.spec;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andreygrigoriev.yfin.model.Actions;
import io.github.andreygrigoriev.yfin.model.CapitalGain;
import io.github.andreygrigoriev.yfin.model.Dividend;
import io.github.andreygrigoriev.yfin.model.Split;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActionRowTest {
    private static final ZoneId NY = ZoneId.of("America/New_York");

    private static ZonedDateTime day(int month, int day) {
        return ZonedDateTime.of(2026, month, day, 9, 30, 0, 0, NY);
    }

    @Test
    void mergesEventsByDayOldestFirst() {
        Actions actions = new Actions(
                List.of(new Dividend(day(3, 2), 0.25), new Dividend(day(1, 5), 0.2)),
                List.of(new Split(day(3, 2), 4, 1)),
                List.of(new CapitalGain(day(2, 1), 0.1)));

        assertThat(ActionRow.of(actions))
                .containsExactly(
                        new ActionRow(LocalDate.of(2026, 1, 5), 0.2, null, null),
                        new ActionRow(LocalDate.of(2026, 2, 1), null, null, 0.1),
                        new ActionRow(LocalDate.of(2026, 3, 2), 0.25, 4.0, null));
    }

    @Test
    void sameDayAmountsOfOneKindAddUp() {
        Actions actions =
                new Actions(List.of(new Dividend(day(3, 2), 0.25), new Dividend(day(3, 2), 0.5)), List.of(), List.of());

        assertThat(ActionRow.of(actions)).containsExactly(new ActionRow(LocalDate.of(2026, 3, 2), 0.75, null, null));
    }

    @Test
    void noEventsNoRows() {
        assertThat(ActionRow.of(new Actions(List.of(), List.of(), List.of()))).isEmpty();
    }
}
