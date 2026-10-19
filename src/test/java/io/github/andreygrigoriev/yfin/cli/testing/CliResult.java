package io.github.andreygrigoriev.yfin.cli.testing;

/**
 * What one CLI invocation produced.
 *
 * @param exitCode the exit code
 * @param out everything written to stdout
 * @param err everything written to stderr
 */
public record CliResult(int exitCode, String out, String err) {
    /**
     * The golden-file form: the exit code, then stdout, then stderr (only when non-empty).
     *
     * @return the rendered result
     */
    public String render() {
        StringBuilder sb = new StringBuilder("exit: ").append(exitCode).append('\n');
        sb.append("--- stdout\n").append(out);
        if (!err.isEmpty()) {
            sb.append("--- stderr\n").append(err);
        }
        return sb.toString();
    }
}
