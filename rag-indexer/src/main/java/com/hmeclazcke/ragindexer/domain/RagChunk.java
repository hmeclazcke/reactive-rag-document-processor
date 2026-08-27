package com.hmeclazcke.ragindexer.domain;

public record RagChunk(
        String id,
        String datasetId,
        int sourceChunkIndex,
        int ragChunkIndex,
        String text,
        long startByteInclusive,
        long endByteExclusive
) {

    private static final String ID_VALIDATION_MESSAGE =
            "id must not be blank";

    private static final String DATASET_ID_VALIDATION_MESSAGE =
            "datasetId must not be blank";

    private static final String SOURCE_CHUNK_INDEX_VALIDATION_MESSAGE =
            "sourceChunkIndex must be zero or greater";

    private static final String RAG_CHUNK_INDEX_VALIDATION_MESSAGE =
            "ragChunkIndex must be zero or greater";

    private static final String TEXT_VALIDATION_MESSAGE =
            "text must not be blank";

    private static final String START_BYTE_INCLUSIVE_VALIDATION_MESSAGE =
            "startByteInclusive must be zero or greater";

    private static final String END_BYTE_EXCLUSIVE_VALIDATION_MESSAGE =
            "endByteExclusive must be greater than startByteInclusive";

    public RagChunk {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(ID_VALIDATION_MESSAGE);
        }

        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException(DATASET_ID_VALIDATION_MESSAGE);
        }

        if (sourceChunkIndex < 0) {
            throw new IllegalArgumentException(SOURCE_CHUNK_INDEX_VALIDATION_MESSAGE);
        }

        if (ragChunkIndex < 0) {
            throw new IllegalArgumentException(RAG_CHUNK_INDEX_VALIDATION_MESSAGE);
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(TEXT_VALIDATION_MESSAGE);
        }

        if (startByteInclusive < 0) {
            throw new IllegalArgumentException(START_BYTE_INCLUSIVE_VALIDATION_MESSAGE);
        }

        if (endByteExclusive <= startByteInclusive) {
            throw new IllegalArgumentException(END_BYTE_EXCLUSIVE_VALIDATION_MESSAGE);
        }
    }
}
