package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.filequeryapi.application.port.out.RagChunkSearchPort;
import com.hmeclazcke.filequeryapi.domain.RagChunk;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchDocumentContextUseCaseTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final String QUESTION = "How does the processor create RAG chunks?";
    private static final int RETRIEVED_CHUNK_LIMIT = 2;

    private final RagChunkSearchPort ragChunkSearch = mock(RagChunkSearchPort.class);
    private final RagChunkQueryPort ragChunkQuery = mock(RagChunkQueryPort.class);

    private final SearchDocumentContextUseCase useCase = new SearchDocumentContextUseCase(
            ragChunkSearch,
            ragChunkQuery,
            RETRIEVED_CHUNK_LIMIT
    );

    @Test
    void returnsRetrievedSourcesInSimilarityOrder() {
        String firstId = "dataset-1g-gemini:rag:0:1";
        String secondId = "dataset-1g-gemini:rag:0:2";

        RagChunk firstChunk = ragChunk(firstId, 1, "The processor accumulates complete lines.");
        RagChunk secondChunk = ragChunk(secondId, 2, "RAG chunks are persisted in MongoDB.");

        when(ragChunkSearch.findSimilarRagChunkIds(DATASET_ID, QUESTION, RETRIEVED_CHUNK_LIMIT))
                .thenReturn(Flux.just(secondId, firstId));

        when(ragChunkQuery.findByDatasetIdAndIds(DATASET_ID, List.of(secondId, firstId)))
                .thenReturn(Flux.just(firstChunk, secondChunk));

        StepVerifier.create(useCase.search(DATASET_ID, QUESTION))
                .expectNext(List.of(
                        new RagChunkSource(
                                1,
                                secondId,
                                0,
                                2,
                                200,
                                250,
                                "RAG chunks are persisted in MongoDB."
                        ),
                        new RagChunkSource(
                                2,
                                firstId,
                                0,
                                1,
                                100,
                                150,
                                "The processor accumulates complete lines."
                        )
                ))
                .verifyComplete();

        verify(ragChunkSearch).findSimilarRagChunkIds(DATASET_ID, QUESTION, RETRIEVED_CHUNK_LIMIT);
        verify(ragChunkQuery).findByDatasetIdAndIds(DATASET_ID, List.of(secondId, firstId));
    }

    @Test
    void returnsNoSourcesWhenSimilaritySearchFindsNoChunks() {
        when(ragChunkSearch.findSimilarRagChunkIds(DATASET_ID, QUESTION, RETRIEVED_CHUNK_LIMIT))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.search(DATASET_ID, QUESTION))
                .expectNext(List.of())
                .verifyComplete();

        verify(ragChunkSearch).findSimilarRagChunkIds(DATASET_ID, QUESTION, RETRIEVED_CHUNK_LIMIT);
        verifyNoInteractions(ragChunkQuery);
    }

    @Test
    void failsWhenDatasetIdIsBlank() {
        StepVerifier.create(useCase.search(" ", QUESTION))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException
                                && error.getMessage().equals("datasetId must not be blank")
                )
                .verify();
    }

    @Test
    void failsWhenQuestionIsBlank() {
        StepVerifier.create(useCase.search(DATASET_ID, " "))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException
                                && error.getMessage().equals("question must not be blank")
                )
                .verify();
    }

    @Test
    void failsWhenRetrievedChunkLimitIsZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchDocumentContextUseCase(ragChunkSearch, ragChunkQuery, 0)
        );
    }

    private RagChunk ragChunk(String id, int ragChunkIndex, String text) {
        return new RagChunk(
                id,
                DATASET_ID,
                0,
                ragChunkIndex,
                text,
                ragChunkIndex * 100L,
                ragChunkIndex * 100L + 50
        );
    }
}
