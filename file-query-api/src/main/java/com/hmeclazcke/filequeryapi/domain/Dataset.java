package com.hmeclazcke.filequeryapi.domain;

public record Dataset(
        String datasetId,
        String path,
        long fileSizeBytes,
        long chunkSizeBytes,
        int chunkCount
) {
}
