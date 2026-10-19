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
- CI: `ci.yml` runs `verify` on ubuntu-24.04.

## Layout

- JPMS module `io.github.andreygrigoriev.yfin.cli`. It `requires io.github.andreygrigoriev.yfin`,
  `info.picocli`, `com.fasterxml.jackson.core` and `static org.jspecify`. Packages with picocli
  command classes are `opens ... to info.picocli`.
- `Main` runs `YfinCommand` (the root command) and exits with its code.
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
  metadata.
- Lookup tables are Java constants, not runtime resources.
- Every package that contains classes has a `package-info.java` with `@NullMarked` (JSpecify).
- Javadoc lint is `all,-missing`, and its warnings fail the build. Watch for broken `{@link}`
  references and bad HTML.
- Data goes to stdout, diagnostics to stderr. Exit codes are fixed (see the plan): 0 success,
  1 unexpected, 2 usage.

## Tests

- Unit tests never use the network. Use `FakeTransport` and the recorded fixtures from the
  `yfin-core` test-jar (`fixtures/<endpoint>/<case>.json`).
- Typical setup: `fake.onGet("").respond(404, "not found")` (cookie bootstrap),
  `fake.onGet("/v1/test/getcrumb").respond(200, "crumb1")`, then
  `YFinance.builder().transport(fake).cache(...).maxRetries(0).build()`.
- Command tests run `new CommandLine(new YfinCommand()).setOut(...).setErr(...).execute(args)`
  and assert on stdout, stderr and the exit code.

## Housekeeping

- Never commit `.idea/` (it is git-ignored).
