package com.hmeclazcke.ragindexer.application;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkIndexPort;
import com.hmeclazcke.ragindexer.domain.IndexRagChunksResult;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexRagChunksUseCaseTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final int BATCH_SIZE = 2;

    private final RagChunkQueryPort ragChunkQuery = mock(RagChunkQueryPort.class);
    private final RagChunkIndexPort ragChunkIndex = mock(RagChunkIndexPort.class);
    private final IndexRagChunksUseCase useCase = new IndexRagChunksUseCase(
            ragChunkQuery,
            ragChunkIndex
    );

    @Test
    void indexesRagChunksInBatches() {
        RagChunk first = ragChunk("dataset-1g-gemini:rag:0:0", 0);
        RagChunk second = ragChunk("dataset-1g-gemini:rag:0:1", 1);
        RagChunk third = ragChunk("dataset-1g-gemini:rag:0:2", 2);
        List<RagChunk> firstBatch = List.of(first, second);
        List<RagChunk> secondBatch = List.of(third);

        when(ragChunkQuery.findByDatasetId(DATASET_ID, BATCH_SIZE)).thenReturn(Flux.just(first, second, third));
        when(ragChunkIndex.indexAll(firstBatch)).thenReturn(Mono.empty());
        when(ragChunkIndex.indexAll(secondBatch)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.index(DATASET_ID, BATCH_SIZE))
                .expectNext(new IndexRagChunksResult(3))
                .verifyComplete();

        verify(ragChunkQuery).findByDatasetId(DATASET_ID, BATCH_SIZE);

        InOrder inOrder = inOrder(ragChunkIndex);
        inOrder.verify(ragChunkIndex).indexAll(firstBatch);
        inOrder.verify(ragChunkIndex).indexAll(secondBatch);
    }

    @Test
    void failsWhenDatasetIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> useCase.index(" ", BATCH_SIZE));
    }

    @Test
    void failsWhenBatchSizeIsZero() {
        assertThrows(IllegalArgumentException.class, () -> useCase.index(DATASET_ID, 0));
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
