package com.hmeclazcke.filequeryapi.domain;

import java.util.List;
import java.util.Objects;

public record DocumentAnswer(
        String answer,
        List<RagChunkSource> sources
) {

    public DocumentAnswer {
        Objects.requireNonNull(answer, "answer must not be null");
        Objects.requireNonNull(sources, "sources must not be null");
        sources = List.copyOf(sources);
    }

    public DocumentAnswer(String answer) {
        this(answer, List.of());
    }
}
