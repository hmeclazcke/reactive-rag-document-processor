package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chunk_word_counts")
public record ChunkWordCountDocument(
        @Id String id,
        String datasetId,
        int chunkIndex,
        String word,
        long count
) {
}