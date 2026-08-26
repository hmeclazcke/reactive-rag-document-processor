package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class WordCountGraphQlController {

    private final GetTopWordsUseCase useCase;

    public WordCountGraphQlController(GetTopWordsUseCase useCase) {
        this.useCase = useCase;
    }

    @QueryMapping
    public Mono<List<WordCount>> topWords(@Argument String datasetId, @Argument int limit) {
        // GraphQL query responses are returned as one JSON document.
        // collectList keeps the pipeline reactive while adapting Flux<WordCount> to Mono<List<WordCount>>.
        return useCase.getTopWords(datasetId, limit).collectList();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleIllegalArgumentException(
            IllegalArgumentException exception,
            DataFetchingEnvironment environment
    ) {
        return GraphqlErrorBuilder.newError(environment)
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .build();
    }
}