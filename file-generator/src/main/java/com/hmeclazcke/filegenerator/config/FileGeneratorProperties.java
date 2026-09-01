package com.hmeclazcke.filegenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "file-generator")
public record FileGeneratorProperties(
        Path datasetPath,
        long minimumSizeBytes,
        String seedResourcePath,
        SeedProvider seedProvider
) {
    public enum SeedProvider {
        LOCAL,
        LLM
    }
}
