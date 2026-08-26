package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import reactor.core.publisher.Flux;

public class GetTopWordsUseCase {

    private static final String LIMIT_VALIDATION_MESSAGE =
            "limit must be greater than zero";

    private static final String MAX_LIMIT_VALIDATION_MESSAGE =
            "limit must be less than or equal to ";

    private final WordCountQueryPort wordCountQuery;
    private final int maxLimit;

    public GetTopWordsUseCase(WordCountQueryPort wordCountQuery, int maxLimit) {
        this.wordCountQuery = wordCountQuery;
        this.maxLimit = maxLimit;
    }

    public Flux<WordCount> getTopWords(String datasetId, int limit) {
        if (limit <= 0) {
            return Flux.error(new IllegalArgumentException(LIMIT_VALIDATION_MESSAGE));
        }

        if (limit > maxLimit) {
            return Flux.error(
                    new IllegalArgumentException(MAX_LIMIT_VALIDATION_MESSAGE + maxLimit)
            );
        }

        return wordCountQuery.findTopWords(datasetId, limit);
    }
}