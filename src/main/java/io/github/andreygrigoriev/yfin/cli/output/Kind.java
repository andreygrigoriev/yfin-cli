package io.github.andreygrigoriev.yfin.cli.output;

/**
 * How a column's values are formatted. One kind drives all three formats; see {@link Values}.
 */
public sealed interface Kind permits Kind.Text, Kind.Integer, Kind.Decimal, Kind.Percent, Kind.Date, Kind.Timestamp {
    /** Free text, printed as is. */
    Kind TEXT = new Text();

    /** Whole numbers such as volume or share counts. */
    Kind INTEGER = new Integer();

    /** A fraction ({@code 0.1234}) shown as a percentage ({@code 12.34%}) in tables. */
    Kind PERCENT = new Percent();

    /** A calendar date. */
    Kind DATE = new Date();

    /** A point in time, shown in its own zone (the exchange zone for bars and events). */
    Kind TIMESTAMP = new Timestamp();

    /**
     * A decimal number shown with a fixed number of fraction digits in tables.
     *
     * @param scale fraction digits, 0 to 10
     * @return the kind
     */
    static Kind decimal(int scale) {
        return new Decimal(scale);
    }

    /** See {@link #TEXT}. */
    record Text() implements Kind {}

    /** See {@link #INTEGER}. */
    record Integer() implements Kind {}

    /**
     * See {@link #decimal(int)}.
     *
     * @param scale fraction digits, 0 to 10
     */
    record Decimal(int scale) implements Kind {
        /** Validates the scale. */
        public Decimal {
            if (scale < 0 || scale > 10) {
                throw new IllegalArgumentException("scale must be 0..10, got " + scale);
            }
        }
    }

    /** See {@link #PERCENT}. */
    record Percent() implements Kind {}

    /** See {@link #DATE}. */
    record Date() implements Kind {}

    /** See {@link #TIMESTAMP}. */
    record Timestamp() implements Kind {}
}
