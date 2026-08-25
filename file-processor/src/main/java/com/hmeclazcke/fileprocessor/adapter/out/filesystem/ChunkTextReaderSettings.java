package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

public record ChunkTextReaderSettings(
        int maxWordLengthBytes,
        int bufferSizeBytes
) {

    private static final String MAX_WORD_LENGTH_BYTES_VALIDATION_MESSAGE =
            "maxWordLengthBytes must be greater than zero";

    private static final String BUFFER_SIZE_BYTES_VALIDATION_MESSAGE =
            "bufferSizeBytes must be greater than zero";

    public ChunkTextReaderSettings {
        if (maxWordLengthBytes <= 0) {
            throw new IllegalArgumentException(MAX_WORD_LENGTH_BYTES_VALIDATION_MESSAGE);
        }

        if (bufferSizeBytes <= 0) {
            throw new IllegalArgumentException(BUFFER_SIZE_BYTES_VALIDATION_MESSAGE);
        }
    }
}