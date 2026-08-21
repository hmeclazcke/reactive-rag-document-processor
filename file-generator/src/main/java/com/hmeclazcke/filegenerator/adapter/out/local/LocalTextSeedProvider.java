package com.hmeclazcke.filegenerator.adapter.out.local;

import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LocalTextSeedProvider implements TextSeedProviderPort {

    private static final String COULD_NOT_LOAD_SEED_TEXTS = "Could not load local seed texts";

    private final List<String> seedLines;
    private int currentIndex;

    public LocalTextSeedProvider(String seedResourcePath) {
        this.seedLines = loadSeedLines(seedResourcePath);
    }

    @Override
    public String nextLine() {
        String line = seedLines.get(currentIndex);
        currentIndex = (currentIndex + 1) % seedLines.size();

        return line + "\n";
    }

    private List<String> loadSeedLines(String seedResourcePath) {
        try (InputStream inputStream = getClass().getResourceAsStream(seedResourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException(COULD_NOT_LOAD_SEED_TEXTS);
            }

            return readLines(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_LOAD_SEED_TEXTS, exception);
        }
    }

    private List<String> readLines(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }
}