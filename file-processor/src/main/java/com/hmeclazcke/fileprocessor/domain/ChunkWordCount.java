package com.hmeclazcke.fileprocessor.domain;

import java.util.Map;

public record ChunkWordCount(
        String datasetId,
        int chunkIndex,
        Map<String, Long> wordCounts
) {
}