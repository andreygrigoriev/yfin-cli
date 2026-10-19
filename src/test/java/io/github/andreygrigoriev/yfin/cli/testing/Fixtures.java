package io.github.andreygrigoriev.yfin.cli.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recorded Yahoo responses from the {@code yfin-core} test-jar ({@code fixtures/<endpoint>/<case>}). */
public final class Fixtures {
    private static final Pattern TIMESTAMPS = Pattern.compile("\"timestamp\":\\s*\\[([\\d,\\s]*)]");
    private static final Pattern TIME_FIELDS = Pattern.compile("\"(regularMarketTime|start|end)\":\\s*(\\d+)");

    private Fixtures() {}

    /**
     * Loads a library fixture from the class path.
     *
     * @param path path below {@code fixtures/}, e.g. {@code quote/aapl.json}
     * @return the fixture body as UTF-8 text
     * @throws AssertionError if there is no such fixture
     */
    public static String load(String path) {
        String resource = "fixtures/" + path;
        try (InputStream in = ClassLoader.getSystemResourceAsStream(resource)) {
            if (in == null) {
                throw new AssertionError("fixture not found on the class path: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read fixture " + resource, e);
        }
    }

    /**
     * Loads a chart fixture with every bar moved by whole days, so that its last bar falls {@code
     * daysAgo} days before today in {@code zone}. Values that look back from "now", such as {@code
     * FastInfo}'s one-year window, then stay the same whatever the date the test runs on. The bar
     * timestamps and the meta times ({@code regularMarketTime}, trading period bounds) move; nothing
     * else changes.
     *
     * @param path chart fixture path below {@code fixtures/}
     * @param zone the exchange zone the fixture's dates are in
     * @param daysAgo how many days before today the last bar should be
     * @return the shifted fixture body
     */
    public static String chartEndingDaysAgo(String path, ZoneId zone, int daysAgo) {
        String body = load(path);
        Matcher bars = TIMESTAMPS.matcher(body);
        if (!bars.find()) {
            throw new AssertionError("no bar timestamps in " + path);
        }
        String[] stamps = bars.group(1).split(",");
        long last = Long.parseLong(stamps[stamps.length - 1].trim());
        LocalDate lastDay = Instant.ofEpochSecond(last).atZone(zone).toLocalDate();
        long shift = ChronoUnit.DAYS.between(lastDay, LocalDate.now(zone).minusDays(daysAgo)) * 86_400;
        String shifted = TIMESTAMPS.matcher(body).replaceAll(m -> {
            StringBuilder sb = new StringBuilder("\"timestamp\":[");
            String[] values = m.group(1).split(",");
            for (int i = 0; i < values.length; i++) {
                sb.append(i == 0 ? "" : ",").append(Long.parseLong(values[i].trim()) + shift);
            }
            return Matcher.quoteReplacement(sb.append(']').toString());
        });
        return TIME_FIELDS
                .matcher(shifted)
                .replaceAll(m -> "\"" + m.group(1) + "\":" + (Long.parseLong(m.group(2)) + shift));
    }
}
