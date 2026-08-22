package com.hmeclazcke.filecoordinator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "file-coordinator")
public record FileCoordinatorProperties(
        Path datasetPath,
        long chunkSizeBytes
) {
}