package io.github.andreygrigoriev.yfin.cli.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Recorded Yahoo responses from the {@code yfin-core} test-jar ({@code fixtures/<endpoint>/<case>}). */
public final class Fixtures {
    private Fixtures() {}

    /**
     * Loads a library fixture from the class path.
     *
     * @param path path below {@code fixtures/}, e.g. {@code quote/aapl.json}
     * @return the fixture body as UTF-8 text
     * @throws AssertionError if there is no such fixture
     */
    public static String load(String path) {
        String resource = "fixtures/" + path;
        try (InputStream in = ClassLoader.getSystemResourceAsStream(resource)) {
            if (in == null) {
                throw new AssertionError("fixture not found on the class path: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read fixture " + resource, e);
        }
    }
}
