package com.hmeclazcke.fileprocessor.domain;

import java.util.Map;

public record ChunkWordCountsComputed(
        Map<String, Long> wordCounts
) implements FileChunkProcessingEvent {

    public ChunkWordCountsComputed {
        wordCounts = Map.copyOf(wordCounts);
    }
}
