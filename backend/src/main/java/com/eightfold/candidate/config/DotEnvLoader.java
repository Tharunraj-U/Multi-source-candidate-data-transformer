package com.eightfold.candidate.config;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DotEnvLoader {

    private DotEnvLoader() {}

    public static void loadIfPresent() {
        Path env = Path.of(".env");
        if (!Files.isRegularFile(env)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(env)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (Exception ignored) {
            // optional local dev convenience
        }
    }
}
