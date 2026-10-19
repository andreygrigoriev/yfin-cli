# yfin-cli

`yfin`, a command-line tool over the published `io.github.andreygrigoriev:yfin-core` library
(repo `andreygrigoriev/yfin`). Java 25, one Maven module, compiled to a GraalVM native image and
shipped through the Homebrew tap `andreygrigoriev/homebrew-tap`. Completed plans live in
`docs/plans/completed/`; the README documents the user-facing behaviour (commands, formats, exit
codes).

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
- Live check: `scripts/live-smoke.sh BINARY` runs a few commands against Yahoo with `--format json`
  and checks the shape with `jq` (keys and types, never values). `nightly-live.yml` runs it on the
  Linux native binary. Locally: `./mvnw -Pnative package -DskipTests && scripts/live-smoke.sh target/yfin`.

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
- Spec style: one `Spec<T>` constant per model in `Specs`, built with
  `Spec.<T>builder().col(name, fn, kind)...build()`, with a javadoc saying which command prints it.
  Column names are snake_case and become the table header, CSV header and JSON key, so treat them
  as a public contract and change goldens deliberately. Use the shared kinds (`PRICE`, `RATIO`,
  `AMOUNT`, `SCORE`) rather than ad-hoc `Kind.decimal(n)`. `PERCENT` columns hold fractions: shift
  Yahoo values that come in percent (e.g. `change_pct` from lookup, screener, options) by 100 in the
  spec, and leave a comment where Yahoo is inconsistent (`dividendYield`). Period-column data
  (statements, valuation) builds a `Spec.dynamic(...)` at runtime in `StatementSpecs`; derived row
  types (`ActionRow`, `OptionRow`) are records in `cli.spec`.
- Output shapes: `Output.list`, `Output.record`, and `Output.lists` with `Section`s for commands that
  print several lists (`search --news`). Validate option values in the command and throw a
  picocli `ParameterException` (exit 2) before any request.
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
- Golden workflow: add the case to the command's test, run it once with `-Dgolden.update=true`,
  read the new file (exit code, stdout, stderr sections) as the spec of the behaviour, then run
  without the flag. Every leaf command has `normal`, `empty` and an error case in `table` and
  `json`, plus `csv` for one case per output shape. Never edit a golden by hand to make a test pass.
- Native gotchas: run `./mvnw -Pnative test` after adding a command, a dependency or a resource.
  The main image needs no hand-written metadata; if native fails, the fix is almost always in our
  code (reflection, a resource) or the test-scoped metadata, not a new main metadata file.
  Anything that prints on stderr in native but not on the JVM (e.g. the slf4j "no providers"
  warning) breaks the stderr goldens, so check both. `-Os` keeps the binary at ~27 MiB with
  ~14 ms startup; re-measure (README "Size and startup") if a change grows it.
- `docs/fixtures.md` maps commands to library fixtures and lists fixtures still to record in yfin.

## Claude skill

- `skills/yfin/SKILL.md` is a user-facing Claude Code skill for the CLI, shipped as a plugin
  through `.claude-plugin/plugin.json` and `.claude-plugin/marketplace.json` (validate with
  `claude plugin validate .`). It names commands, options, JSON keys and exit codes, so update it
  when any of those change, and run its examples against the binary.

## Housekeeping

- Never commit `.idea/` (it is git-ignored).
