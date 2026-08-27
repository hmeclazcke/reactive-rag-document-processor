package com.hmeclazcke.ragindexer.application;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkVectorStorePort;
import com.hmeclazcke.ragindexer.application.port.out.TextEmbeddingPort;
import com.hmeclazcke.ragindexer.domain.IndexRagChunksResult;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import com.hmeclazcke.ragindexer.domain.RagChunkVector;
import com.hmeclazcke.ragindexer.domain.TextEmbedding;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexRagChunksUseCaseTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final int BATCH_SIZE = 2;

    private final RagChunkQueryPort ragChunkQuery = mock(RagChunkQueryPort.class);
    private final TextEmbeddingPort embeddingPort = mock(TextEmbeddingPort.class);
    private final RagChunkVectorStorePort vectorStore = mock(RagChunkVectorStorePort.class);
    private final IndexRagChunksUseCase useCase = new IndexRagChunksUseCase(
            ragChunkQuery,
            embeddingPort,
            vectorStore
    );

    @Test
    void indexesRagChunksInBatches() {
        RagChunk first = ragChunk("dataset-1g-gemini:rag:0:0", 0);
        RagChunk second = ragChunk("dataset-1g-gemini:rag:0:1", 1);
        TextEmbedding firstEmbedding = new TextEmbedding(List.of(0.1F, 0.2F));
        TextEmbedding secondEmbedding = new TextEmbedding(List.of(0.3F, 0.4F));
        List<String> expectedTexts = List.of(first.text(), second.text());
        List<TextEmbedding> embeddings = List.of(firstEmbedding, secondEmbedding);
        List<RagChunkVector> expectedVectors = List.of(
                new RagChunkVector(first.id(), DATASET_ID, first.sourceChunkIndex(), first.ragChunkIndex(), firstEmbedding),
                new RagChunkVector(second.id(), DATASET_ID, second.sourceChunkIndex(), second.ragChunkIndex(), secondEmbedding)
        );

        when(ragChunkQuery.findByDatasetId(DATASET_ID, BATCH_SIZE)).thenReturn(Flux.just(first, second));
        when(embeddingPort.embedAll(expectedTexts)).thenReturn(Mono.just(embeddings));
        when(vectorStore.saveAll(expectedVectors)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.index(DATASET_ID, BATCH_SIZE))
                .expectNext(new IndexRagChunksResult(2))
                .verifyComplete();

        verify(ragChunkQuery).findByDatasetId(DATASET_ID, BATCH_SIZE);
        verify(embeddingPort).embedAll(expectedTexts);
        verify(vectorStore).saveAll(expectedVectors);
    }

    @Test
    void failsWhenDatasetIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> useCase.index(" ", BATCH_SIZE));
    }

    @Test
    void failsWhenBatchSizeIsZero() {
        assertThrows(IllegalArgumentException.class, () -> useCase.index(DATASET_ID, 0));
    }

    @Test
    void failsWhenEmbeddingCountDoesNotMatchRagChunkCount() {
        RagChunk ragChunk = ragChunk("dataset-1g-gemini:rag:0:0", 0);

        when(ragChunkQuery.findByDatasetId(DATASET_ID, BATCH_SIZE)).thenReturn(Flux.just(ragChunk));
        when(embeddingPort.embedAll(List.of(ragChunk.text()))).thenReturn(Mono.just(List.of()));

        StepVerifier.create(useCase.index(DATASET_ID, BATCH_SIZE))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private RagChunk ragChunk(String id, int ragChunkIndex) {
        return new RagChunk(
                id,
                DATASET_ID,
                0,
                ragChunkIndex,
                "java reactor " + ragChunkIndex,
                ragChunkIndex * 100L,
                ragChunkIndex * 100L + 50
        );
    }
}
