package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.DocumentAnswerPort;
import com.hmeclazcke.filequeryapi.domain.DocumentAnswer;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskDocumentUseCaseTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final String QUESTION = "How does the processor create RAG chunks?";

    private final SearchDocumentContextUseCase searchDocumentContext = mock(SearchDocumentContextUseCase.class);
    private final DocumentAnswerPort documentAnswer = mock(DocumentAnswerPort.class);

    private final AskDocumentUseCase useCase = new AskDocumentUseCase(
            searchDocumentContext,
            documentAnswer
    );

    @Test
    void answersQuestionUsingRetrievedContextSources() {
        List<RagChunkSource> sources = List.of(
                new RagChunkSource(
                        1,
                        "dataset-1g-gemini:rag:0:2",
                        0,
                        2,
                        200,
                        250,
                        "RAG chunks are persisted in MongoDB."
                ),
                new RagChunkSource(
                        2,
                        "dataset-1g-gemini:rag:0:1",
                        0,
                        1,
                        100,
                        150,
                        "The processor accumulates complete lines."
                )
        );

        when(searchDocumentContext.search(DATASET_ID, QUESTION))
                .thenReturn(Mono.just(sources));

        when(documentAnswer.answer(QUESTION, sources)).thenReturn(Mono.just(
                "The processor builds RAG chunks from complete lines and stores them in MongoDB."
        ));

        StepVerifier.create(useCase.ask(DATASET_ID, QUESTION))
                .expectNext(new DocumentAnswer(
                        "The processor builds RAG chunks from complete lines and stores them in MongoDB.",
                        sources
                ))
                .verifyComplete();

        verify(searchDocumentContext).search(DATASET_ID, QUESTION);
        verify(documentAnswer).answer(QUESTION, sources);
    }

    @Test
    void returnsNoContextAnswerWhenSearchFindsNoSources() {
        when(searchDocumentContext.search(DATASET_ID, QUESTION))
                .thenReturn(Mono.just(List.of()));

        StepVerifier.create(useCase.ask(DATASET_ID, QUESTION))
                .expectNext(new DocumentAnswer("No relevant context was found for this question."))
                .verifyComplete();

        verify(searchDocumentContext).search(DATASET_ID, QUESTION);
        verifyNoInteractions(documentAnswer);
    }
}
