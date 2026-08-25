package com.hmeclazcke.fileprocessor.domain;

public record FileChunk(
        int index,
        long startByteInclusive,
        long endByteExclusive
) {

    private static final String INDEX_VALIDATION_MESSAGE =
            "index must be zero or greater";

    private static final String START_BYTE_INCLUSIVE_VALIDATION_MESSAGE =
            "startByteInclusive must be zero or greater";

    private static final String END_BYTE_EXCLUSIVE_VALIDATION_MESSAGE =
            "endByteExclusive must be greater than startByteInclusive";

    public FileChunk {
        if (index < 0) {
            throw new IllegalArgumentException(INDEX_VALIDATION_MESSAGE);
        }

        if (startByteInclusive < 0) {
            throw new IllegalArgumentException(START_BYTE_INCLUSIVE_VALIDATION_MESSAGE);
        }

        if (endByteExclusive <= startByteInclusive) {
            throw new IllegalArgumentException(END_BYTE_EXCLUSIVE_VALIDATION_MESSAGE);
        }
    }
}