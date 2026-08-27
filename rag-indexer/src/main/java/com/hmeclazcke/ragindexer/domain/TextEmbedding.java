package com.hmeclazcke.ragindexer.domain;

import java.util.List;

public record TextEmbedding(
        List<Float> values
) {

    private static final String VALUES_VALIDATION_MESSAGE =
            "values must not be empty";

    public TextEmbedding {
        values = List.copyOf(values);

        if (values.isEmpty()) {
            throw new IllegalArgumentException(VALUES_VALIDATION_MESSAGE);
        }
    }
}
