/** The {@code yfin} command-line tool. */
module io.github.andreygrigoriev.yfin.cli {
    requires io.github.andreygrigoriev.yfin;
    requires info.picocli;
    requires com.fasterxml.jackson.core;
    requires static org.jspecify;

    // picocli reads and writes the annotated command fields reflectively
    opens io.github.andreygrigoriev.yfin.cli to
            info.picocli;
    opens io.github.andreygrigoriev.yfin.cli.cmd to
            info.picocli;
}
