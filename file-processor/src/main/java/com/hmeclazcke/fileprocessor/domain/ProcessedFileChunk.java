package com.hmeclazcke.fileprocessor.domain;

public record ProcessedFileChunk(
        ChunkWordCount chunkWordCount,
        long ragChunkCount
) {
}
