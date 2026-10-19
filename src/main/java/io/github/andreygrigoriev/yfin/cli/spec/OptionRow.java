package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.model.OptionChain;
import io.github.andreygrigoriev.yfin.model.OptionContract;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One contract of an {@link OptionChain} with its side and expiration, so that calls and puts fit
 * one table.
 *
 * @param type {@value #CALL} or {@value #PUT}
 * @param expiry the chain's expiration date
 * @param contract the contract
 */
public record OptionRow(String type, LocalDate expiry, OptionContract contract) {
    /** {@link #type()} of a call. */
    public static final String CALL = "call";

    /** {@link #type()} of a put. */
    public static final String PUT = "put";

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if a field is {@code null}
     */
    public OptionRow {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(expiry, "expiry");
        Objects.requireNonNull(contract, "contract");
    }

    /**
     * The rows of a chain: the calls, then the puts, each by strike as Yahoo orders them.
     *
     * @param chain the chain
     * @param calls whether to include the calls
     * @param puts whether to include the puts
     * @return the rows
     */
    public static List<OptionRow> of(OptionChain chain, boolean calls, boolean puts) {
        List<OptionRow> rows = new ArrayList<>();
        if (calls) {
            chain.calls().forEach(c -> rows.add(new OptionRow(CALL, chain.expiration(), c)));
        }
        if (puts) {
            chain.puts().forEach(c -> rows.add(new OptionRow(PUT, chain.expiration(), c)));
        }
        return List.copyOf(rows);
    }
}
