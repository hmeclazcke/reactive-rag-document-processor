package com.hmeclazcke.fileprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "file-processor")
public record FileProcessorProperties(
        Path datasetPath,
        String datasetId,
        int chunkIndex,
        long startByteInclusive,
        long endByteExclusive,
        int maxLineLengthBytes,
        int bufferSizeBytes,
        int ragChunkMaxTextLengthCharacters,
        int ragChunkBatchSize
) {
}
