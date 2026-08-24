package com.hmeclazcke.fileprocessor.domain;

import java.util.Map;

public record ChunkWordCount(
        int chunkIndex,
        Map<String, Long> wordCounts
) {
}