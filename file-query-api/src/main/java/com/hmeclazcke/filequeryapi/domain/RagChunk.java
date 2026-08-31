package com.hmeclazcke.filequeryapi.domain;

public record RagChunk(
        String id,
        String datasetId,
        int sourceChunkIndex,
        int ragChunkIndex,
        String text,
        long startByteInclusive,
        long endByteExclusive
) {
}
