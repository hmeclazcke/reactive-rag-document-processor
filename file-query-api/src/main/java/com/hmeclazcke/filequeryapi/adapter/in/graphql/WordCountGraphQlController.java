package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class WordCountGraphQlController {

    private final GetTopWordsUseCase useCase;

    public WordCountGraphQlController(GetTopWordsUseCase useCase) {
        this.useCase = useCase;
    }

    @QueryMapping
    public Flux<WordCount> topWords(@Argument int limit) {
        return useCase.getTopWords(limit);
    }
}