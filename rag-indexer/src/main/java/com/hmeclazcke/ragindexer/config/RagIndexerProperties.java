package com.hmeclazcke.ragindexer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag-indexer")
public record RagIndexerProperties(
        String datasetId,
        int batchSize
) {
}
