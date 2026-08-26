package com.hmeclazcke.filegenerator.adapter.out.llm;

import com.hmeclazcke.filegenerator.application.port.out.GeneratedSeedTextPort;
import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;

import java.util.List;

public class LlmTextSeedProvider implements TextSeedProviderPort {

    private static final String GENERATED_SEED_TEXT_MUST_CONTAIN_LINES =
            "generated seed text must contain at least one non-blank line";
    private final List<String> seedLines;
    private int currentIndex;

    public LlmTextSeedProvider(GeneratedSeedTextPort generatedSeedTextPort) {
        List<String> generatedSeedLines = generatedSeedTextPort.generate()
                .lines()
                .filter(line -> !line.isBlank())
                .toList();

        if (generatedSeedLines.isEmpty()) {
            throw new IllegalArgumentException(GENERATED_SEED_TEXT_MUST_CONTAIN_LINES);
        }

        this.seedLines = generatedSeedLines;
    }

    @Override
    public String nextLine() {
        String line = seedLines.get(currentIndex);

        // Cycle through the generated seed lines so a small LLM response can produce a large dataset.
        currentIndex = (currentIndex + 1) % seedLines.size();

        // Business rule: generated datasets must be line-delimited, so every seed line ends with '\n'.
        return line + "\n";
    }
}