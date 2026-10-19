# yfin-cli

`yfin` is a command-line tool for Yahoo Finance market data, built on the
[yfin](https://github.com/andreygrigoriev/yfin) Java library and compiled to a GraalVM native
image.

Work in progress: the commands are being added (see `docs/plans/`).

## Install

```sh
brew install andreygrigoriev/tap/yfin
```

## Build

Java 25 and the Maven wrapper:

```sh
./mvnw verify
```

Native image, with GraalVM 25 as `JAVA_HOME`:

```sh
./mvnw -Pnative package   # builds target/yfin
./mvnw -Pnative test      # runs the test suite as a native image
```

Measured on an Apple Silicon Mac (GraalVM 25, `-Os`): `target/yfin` is 27.5 MiB, and
`yfin --version` starts in about 14 ms (median of 30 runs).

## License

Apache License 2.0. See `LICENSE` and `NOTICE`. Not affiliated with Yahoo, Inc.
