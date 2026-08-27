package com.hmeclazcke.fileprocessor.adapter.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rag_chunks")
public record RagChunkDocument(
        @Id String id,
        String datasetId,
        int sourceChunkIndex,
        int ragChunkIndex,
        String text,
        long startByteInclusive,
        long endByteExclusive
) {
}
