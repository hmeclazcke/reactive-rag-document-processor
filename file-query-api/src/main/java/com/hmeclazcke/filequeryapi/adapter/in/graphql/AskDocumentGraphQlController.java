package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.AskDocumentUseCase;
import com.hmeclazcke.filequeryapi.application.DocumentAnswerGenerationException;
import com.hmeclazcke.filequeryapi.application.SearchDocumentContextUseCase;
import com.hmeclazcke.filequeryapi.domain.DocumentAnswer;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
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
public class AskDocumentGraphQlController {

    private final AskDocumentUseCase useCase;
    private final SearchDocumentContextUseCase searchDocumentContext;

    public AskDocumentGraphQlController(
            AskDocumentUseCase useCase,
            SearchDocumentContextUseCase searchDocumentContext
    ) {
        this.useCase = useCase;
        this.searchDocumentContext = searchDocumentContext;
    }

    @QueryMapping
    public Mono<DocumentAnswer> askDocument(
            @Argument String datasetId,
            @Argument String question
    ) {
        return useCase.ask(datasetId, question);
    }

    @QueryMapping
    public Mono<List<RagChunkSource>> searchDocumentContext(
            @Argument String datasetId,
            @Argument String question
    ) {
        return searchDocumentContext.search(datasetId, question);
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

    @GraphQlExceptionHandler
    public GraphQLError handleDocumentAnswerGenerationException(
            DocumentAnswerGenerationException exception,
            DataFetchingEnvironment environment
    ) {
        return GraphqlErrorBuilder.newError(environment)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(exception.getMessage())
                .build();
    }
}
