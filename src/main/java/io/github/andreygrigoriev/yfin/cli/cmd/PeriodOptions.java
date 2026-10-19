package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.request.Frequency;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Option;

/** The mutually exclusive {@code --quarterly|--ttm} options; neither means annual. */
final class PeriodOptions {
    @Option(names = "--quarterly", required = true, description = "Fiscal quarters instead of fiscal years.")
    boolean quarterly;

    @Option(names = "--ttm", required = true, description = "The latest trailing twelve months instead.")
    boolean ttm;

    /**
     * The frequency the options select.
     *
     * @param options the parsed group, {@code null} when neither option was given
     * @return {@link Frequency#QUARTERLY}, {@link Frequency#TRAILING} or {@link Frequency#ANNUAL}
     */
    static Frequency frequency(@Nullable PeriodOptions options) {
        if (options == null) {
            return Frequency.ANNUAL;
        }
        return options.quarterly ? Frequency.QUARTERLY : options.ttm ? Frequency.TRAILING : Frequency.ANNUAL;
    }
}
