package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "datasets")
public record DatasetDocument(
        @Id String id,
        String path,
        long fileSizeBytes,
        long chunkSizeBytes,
        int chunkCount
) {
}