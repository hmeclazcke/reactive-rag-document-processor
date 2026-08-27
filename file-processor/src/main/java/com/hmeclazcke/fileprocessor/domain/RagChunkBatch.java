package com.hmeclazcke.fileprocessor.domain;

import java.util.List;

public record RagChunkBatch(
        List<RagChunk> ragChunks
) implements FileChunkProcessingEvent {

    private static final String RAG_CHUNKS_VALIDATION_MESSAGE =
            "ragChunks must not be empty";

    public RagChunkBatch {
        // Copy the batch so downstream persistence cannot mutate the emitted processing result.
        ragChunks = List.copyOf(ragChunks);

        if (ragChunks.isEmpty()) {
            throw new IllegalArgumentException(RAG_CHUNKS_VALIDATION_MESSAGE);
        }
    }

    public int size() {
        return ragChunks.size();
    }
}
