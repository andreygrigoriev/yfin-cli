# yfin-cli

`yfin`, a command-line tool over the published `io.github.andreygrigoriev:yfin-core` library
(repo `andreygrigoriev/yfin`). Java 25, one Maven module, compiled to a GraalVM native image and
shipped through the Homebrew tap `andreygrigoriev/homebrew-tap`. The implementation plan lives in
`docs/plans/`.

## Build commands

Always use the wrapper (`./mvnw`), never a bare `mvn`.

- `./mvnw verify`: unit tests, enforcer, Spotless check (palantir-java-format) and javadoc lint.
  Must pass before any commit.
- `./mvnw spotless:apply`: fix formatting. Run it whenever `verify` fails on Spotless.
- `./mvnw -Pnative package`: builds the `target/yfin` native image (`--no-fallback`, `-Os`).
  `./mvnw -Pnative test`: the test suite as a native image. Both need GraalVM 25; locally:
  `JAVA_HOME=~/.sdkman/candidates/java/25-graal ./mvnw -Pnative test`. Run it after changes to
  dependencies, resources or anything picocli reads reflectively.
- CI: `ci.yml` runs `verify` and a GraalVM `-Pnative test` job on ubuntu-24.04, plus
  `packaging/homebrew/test-render.sh` (formula render check).
- Releases: push a `vX.Y.Z` tag. `release.yml` builds the native binary on 4 runners
  (`-Drevision=X.Y.Z`), smoke-tests `--version`/`--help`, publishes the tarballs and `SHA256SUMS`
  as a GitHub release, then renders `packaging/homebrew/yfin.rb.tmpl` into
  `andreygrigoriev/homebrew-tap` (`TAP_TOKEN` secret, the only one). `workflow_dispatch` with
  `dry_run` builds and smoke-tests only.

## Layout

- JPMS module `io.github.andreygrigoriev.yfin.cli`. It `requires io.github.andreygrigoriev.yfin`,
  `info.picocli`, `com.fasterxml.jackson.core` and `static org.jspecify`. Packages with picocli
  command classes are `opens ... to info.picocli`.
- `Main` runs `YfinCommand` (the root command) and exits with its code. `YfinCommand` owns the
  invocation state: `GlobalOptions` (a mixin of `ScopeType.INHERIT` options, bound to the root's
  one instance), a lazily built `YFinance` from its `ClientFactory` (closed by `close()`), and
  `output()`. Subcommands reach it with `YfinCommand.root(spec)`.
- Build command lines with `YfinCommand.commandLine(root)`: it registers `Converters` after the
  subcommands exist (picocli doesn't apply converters to subcommands added later).
- `src/main/java-templates/.../BuildInfo.java` is filtered by `templating-maven-plugin`
  (`VERSION`, `YFIN_CORE_VERSION`). Edit the template, never `target/generated-sources`.

## Conventions

- Use only the library's exported API (`yfin`, `yfin.model`, `yfin.request`, `yfin.spi`,
  `yfin.exception`); the module system enforces it. If something is missing, fix it in the
  library rather than working around it here.
- `yfin-core` comes from Central (`yfin-core.version` in the pom). CLI and library releases are
  independent.
- GraalVM: no reflection, classpath scanning, dynamic proxies or Jackson databind in our own code.
  picocli's reflection is covered by the `picocli-codegen` annotation processor, which generates
  `META-INF/native-image/picocli-generated/...` at compile time; there is no hand-written
  main metadata. The only hand-written file is test-scoped
  (`src/test/resources/META-INF/native-image/.../yfin-cli-tests`): it includes the library's
  `fixtures/**` in the native test image.
- `slf4j-nop` is a runtime dependency so yfin-core's slf4j-api doesn't print the
  "no providers" warning on stderr.
- Lookup tables are Java constants, not runtime resources.
- Every package that contains classes has a `package-info.java` with `@NullMarked` (JSpecify).
- Javadoc lint is `all,-missing`, and its warnings fail the build. Watch for broken `{@link}`
  references and bad HTML.
- Data goes to stdout, diagnostics to stderr. Exit codes are fixed constants in `ExitCodes`
  (0 success, 1 unexpected, 2 usage, 3-7 library errors, 130 interrupted). Commands let library
  exceptions propagate: `ErrorHandler` (registered by `YfinCommand.commandLine`) prints
  `yfin: [<symbol>: ]<reason>` and maps the exit code.
- Commands live in `cli.cmd` and are listed in `YfinCommand`'s `subcommands`; column specs of
  library models are constants in `cli.spec.Specs`. The root's `@Command(scope = INHERIT)` gives
  every subcommand `--help`/`--version`.
- Commands over several symbols use `MultiSymbol.fetch(symbols, fn)` (virtual threads, input
  order kept), print the successes, then `return result.reportFailures(err, verbose)`.

## Tests

- Unit tests never use the network. Use `FakeTransport` and the recorded fixtures from the
  `yfin-core` test-jar (`fixtures/<endpoint>/<case>.json`).
- Typical setup: `fake.onGet("").respond(404, "not found")` (cookie bootstrap),
  `fake.onGet("/v1/test/getcrumb").respond(200, "crumb1")`, then
  `YFinance.builder().transport(fake).cache(...).maxRetries(0).build()`.
- Command tests use `testing.CliRunner`: it registers that bootstrap on a `FakeTransport`, runs
  `yfin` in-process (piped terminal, width 120, no cache, no retries) and returns a `CliResult`
  (exit code, stdout, stderr). Register responses with `cli.fixture(path, "quote/aapl.json")` or
  `cli.onGet(path)...`; `Fixtures.load` reads library fixtures from the class path. Statements are
  fetched in chunks: `cli.timeseries(symbol, "timeseries/aapl-annual-financials")` serves each
  `-<n>.json` chunk to the request whose first type it holds.
- Compare results with `Golden.assertMatches(result, "<command path>/<case>.<format>")`, files under
  `src/test/resources/golden/`. `./mvnw test -Dgolden.update=true` rewrites them; review the diff.
- Library code that looks back from "now" (e.g. `FastInfo`'s one-year window) can't take a test
  clock through the exported API: serve such charts with `Fixtures.chartEndingDaysAgo(path, zone,
  days)`, which shifts the bars so the goldens don't drift with the date.
- `GoldenCoverageTest` fails if a leaf command (except `help`, `completion`) lacks a golden
  directory with a `normal.*` case exiting 0 and a case exiting non-zero.
- `docs/fixtures.md` maps commands to library fixtures and lists fixtures still to record in yfin.

## Housekeeping

- Never commit `.idea/` (it is git-ignored).
