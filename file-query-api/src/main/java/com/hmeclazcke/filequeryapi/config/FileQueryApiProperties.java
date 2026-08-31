package com.hmeclazcke.filequeryapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file-query-api")
public record FileQueryApiProperties(
        TopWords topWords,
        AskDocument askDocument
) {

    public record TopWords(
            int maxLimit
    ) {
    }

    public record AskDocument(
            int retrievedChunkLimit
    ) {
    }
}
