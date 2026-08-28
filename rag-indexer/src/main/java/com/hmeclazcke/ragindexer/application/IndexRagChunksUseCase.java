package com.hmeclazcke.ragindexer.application;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkIndexPort;
import com.hmeclazcke.ragindexer.domain.IndexRagChunksResult;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import reactor.core.publisher.Mono;

import java.util.List;

public class IndexRagChunksUseCase {

    private static final String DATASET_ID_VALIDATION_MESSAGE =
            "datasetId must not be blank";

    private static final String BATCH_SIZE_VALIDATION_MESSAGE =
            "batchSize must be greater than zero";

    private final RagChunkQueryPort ragChunkQuery;
    private final RagChunkIndexPort ragChunkIndex;

    public IndexRagChunksUseCase(
            RagChunkQueryPort ragChunkQuery,
            RagChunkIndexPort ragChunkIndex
    ) {
        this.ragChunkQuery = ragChunkQuery;
        this.ragChunkIndex = ragChunkIndex;
    }

    public Mono<IndexRagChunksResult> index(String datasetId, int batchSize) {
        validate(datasetId, batchSize);

        return ragChunkQuery.findByDatasetId(datasetId, batchSize)
                .buffer(batchSize)
                .concatMap(this::indexBatch, 1)
                .reduce(0L, Long::sum)
                .map(IndexRagChunksResult::new);
    }

    private void validate(String datasetId, int batchSize) {
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException(DATASET_ID_VALIDATION_MESSAGE);
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(BATCH_SIZE_VALIDATION_MESSAGE);
        }
    }

    private Mono<Long> indexBatch(List<RagChunk> ragChunks) {
        return ragChunkIndex.indexAll(ragChunks)
                .thenReturn((long) ragChunks.size());
    }
}
