package com.hmeclazcke.fileprocessor.domain;

import java.util.Map;

public record PartialWordCount(
        int chunkIndex,
        Map<String, Long> wordCounts
) {
}