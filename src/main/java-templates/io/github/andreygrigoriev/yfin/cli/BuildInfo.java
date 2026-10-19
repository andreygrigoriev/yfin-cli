package io.github.andreygrigoriev.yfin.cli;

/** Versions filled in at build time by the templating plugin. */
final class BuildInfo {
    /** Version of the CLI itself. */
    static final String VERSION = "${project.version}";

    /** Version of the yfin-core library the CLI was built against. */
    static final String YFIN_CORE_VERSION = "${yfin-core.version}";

    private BuildInfo() {}
}
