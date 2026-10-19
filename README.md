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

## License

Apache License 2.0. See `LICENSE` and `NOTICE`. Not affiliated with Yahoo, Inc.
