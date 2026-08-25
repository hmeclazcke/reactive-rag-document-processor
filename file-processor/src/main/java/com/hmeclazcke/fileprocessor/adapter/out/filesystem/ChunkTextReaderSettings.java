package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

public record ChunkTextReaderSettings(
        int maxLineLengthBytes,
        int bufferSizeBytes
) {

    private static final String MAX_LINE_LENGTH_BYTES_VALIDATION_MESSAGE =
            "maxLineLengthBytes must be greater than zero";

    private static final String BUFFER_SIZE_BYTES_VALIDATION_MESSAGE =
            "bufferSizeBytes must be greater than zero";

    public ChunkTextReaderSettings {
        if (maxLineLengthBytes <= 0) {
            throw new IllegalArgumentException(MAX_LINE_LENGTH_BYTES_VALIDATION_MESSAGE);
        }

        if (bufferSizeBytes <= 0) {
            throw new IllegalArgumentException(BUFFER_SIZE_BYTES_VALIDATION_MESSAGE);
        }
    }
}