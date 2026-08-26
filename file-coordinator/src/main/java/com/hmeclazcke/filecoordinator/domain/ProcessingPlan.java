package com.hmeclazcke.filecoordinator.domain;

import java.nio.file.Path;
import java.util.List;

public record ProcessingPlan(
        String datasetId,
        Path datasetPath,
        long fileSizeBytes,
        long chunkSizeBytes,
        List<FileChunk> chunks
) {
}