package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

public record FileChunkProcessorSettings(
        int maxLineLengthBytes,
        int bufferSizeBytes,
        int ragChunkMaxTextLengthCharacters,
        int ragChunkBatchSize
) {

    private static final String MAX_LINE_LENGTH_BYTES_VALIDATION_MESSAGE =
            "maxLineLengthBytes must be greater than zero";

    private static final String BUFFER_SIZE_BYTES_VALIDATION_MESSAGE =
            "bufferSizeBytes must be greater than zero";

    private static final String RAG_CHUNK_MAX_TEXT_LENGTH_CHARACTERS_VALIDATION_MESSAGE =
            "ragChunkMaxTextLengthCharacters must be greater than zero";

    private static final String RAG_CHUNK_BATCH_SIZE_VALIDATION_MESSAGE =
            "ragChunkBatchSize must be greater than zero";

    public FileChunkProcessorSettings {
        if (maxLineLengthBytes <= 0) {
            throw new IllegalArgumentException(MAX_LINE_LENGTH_BYTES_VALIDATION_MESSAGE);
        }

        if (bufferSizeBytes <= 0) {
            throw new IllegalArgumentException(BUFFER_SIZE_BYTES_VALIDATION_MESSAGE);
        }

        if (ragChunkMaxTextLengthCharacters <= 0) {
            throw new IllegalArgumentException(RAG_CHUNK_MAX_TEXT_LENGTH_CHARACTERS_VALIDATION_MESSAGE);
        }

        if (ragChunkBatchSize <= 0) {
            throw new IllegalArgumentException(RAG_CHUNK_BATCH_SIZE_VALIDATION_MESSAGE);
        }
    }
}
