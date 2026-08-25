package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import reactor.core.publisher.Flux;

public class GetTopWordsUseCase {

    private static final String LIMIT_VALIDATION_MESSAGE =
            "limit must be greater than zero";

    private final WordCountQueryPort wordCountQuery;

    public GetTopWordsUseCase(WordCountQueryPort wordCountQuery) {
        this.wordCountQuery = wordCountQuery;
    }

    public Flux<WordCount> getTopWords(int limit) {
        if (limit <= 0) {
            return Flux.error(new IllegalArgumentException(LIMIT_VALIDATION_MESSAGE));
        }

        return wordCountQuery.findTopWords(limit);
    }
}