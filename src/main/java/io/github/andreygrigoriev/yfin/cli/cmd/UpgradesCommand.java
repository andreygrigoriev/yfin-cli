package io.github.andreygrigoriev.yfin.cli.cmd;

import io.github.andreygrigoriev.yfin.Ticker;
import io.github.andreygrigoriev.yfin.cli.output.Output;
import io.github.andreygrigoriev.yfin.cli.spec.Specs;
import picocli.CommandLine.Command;

/** {@code yfin upgrades SYMBOL}: analyst rating changes, newest first ({@link Ticker#upgradesDowngrades()}). */
@Command(name = "upgrades", description = "Analyst upgrades and downgrades, newest first.")
public final class UpgradesCommand extends SymbolCommand {
    @Override
    void print(Ticker ticker, Output output) {
        output.list(Specs.UPGRADE_DOWNGRADE, ticker.upgradesDowngrades());
    }
}
