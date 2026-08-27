package com.hmeclazcke.ragindexer.domain;

public record RagChunkVector(
        String id,
        String datasetId,
        int sourceChunkIndex,
        int ragChunkIndex,
        TextEmbedding embedding
) {
}
