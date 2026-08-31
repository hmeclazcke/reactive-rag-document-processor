package com.hmeclazcke.filequeryapi.domain;

import java.util.Objects;

public record RagChunkSource(
        int rank,
        String ragChunkId,
        int sourceChunkIndex,
        int ragChunkIndex,
        long startByteInclusive,
        long endByteExclusive,
        String text
) {

    private static final int TEXT_PREVIEW_MAX_LENGTH = 500;

    public RagChunkSource {
        Objects.requireNonNull(ragChunkId, "ragChunkId must not be null");
        Objects.requireNonNull(text, "text must not be null");
    }

    public String textPreview() {
        if (text.length() <= TEXT_PREVIEW_MAX_LENGTH) {
            return text;
        }

        return text.substring(0, TEXT_PREVIEW_MAX_LENGTH - 3) + "...";
    }
}
