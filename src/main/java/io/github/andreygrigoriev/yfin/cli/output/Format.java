package io.github.andreygrigoriev.yfin.cli.output;

/** Output format chosen with {@code --format}. It is never inferred from the terminal. */
public enum Format {
    /** Aligned, humanised columns for people. */
    TABLE,
    /** RFC 4180 CSV with a header row and raw values. */
    CSV,
    /** A JSON array of objects (lists) or a JSON object (records), with typed values. */
    JSON
}
