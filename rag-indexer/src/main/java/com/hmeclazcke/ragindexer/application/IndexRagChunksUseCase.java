package com.hmeclazcke.ragindexer.application;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkVectorStorePort;
import com.hmeclazcke.ragindexer.application.port.out.TextEmbeddingPort;
import com.hmeclazcke.ragindexer.domain.IndexRagChunksResult;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import com.hmeclazcke.ragindexer.domain.RagChunkVector;
import com.hmeclazcke.ragindexer.domain.TextEmbedding;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class IndexRagChunksUseCase {

    private static final String DATASET_ID_VALIDATION_MESSAGE =
            "datasetId must not be blank";

    private static final String BATCH_SIZE_VALIDATION_MESSAGE =
            "batchSize must be greater than zero";

    private static final String EMBEDDING_COUNT_DOES_NOT_MATCH_RAG_CHUNK_COUNT =
            "embedding count does not match rag chunk count";

    private final RagChunkQueryPort ragChunkQuery;
    private final TextEmbeddingPort embeddingPort;
    private final RagChunkVectorStorePort vectorStore;

    public IndexRagChunksUseCase(
            RagChunkQueryPort ragChunkQuery,
            TextEmbeddingPort embeddingPort,
            RagChunkVectorStorePort vectorStore
    ) {
        this.ragChunkQuery = ragChunkQuery;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
    }

    public Mono<IndexRagChunksResult> index(String datasetId, int batchSize) {
        validate(datasetId, batchSize);

        // Flux keeps the Mongo cursor lazy: the use case consumes rag_chunks progressively
        // instead of materializing the whole dataset in memory.
        Flux<RagChunk> ragChunksFromMongo = ragChunkQuery.findByDatasetId(datasetId, batchSize);

        // Group chunks before calling the embedding model and the vector store.
        Flux<List<RagChunk>> ragChunkBatches = ragChunksFromMongo.buffer(batchSize);

        // Index one batch at a time. Each emitted Long is the number of chunks indexed in that batch.
        Flux<Long> indexedBatchCounts = ragChunkBatches.concatMap(this::indexBatch, 1);

        // Sum all batch counts and return the final CLI/job result.
        Mono<Long> totalIndexedChunks = indexedBatchCounts.reduce(0L, Long::sum);

        return totalIndexedChunks.map(IndexRagChunksResult::new);
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
        // The embedding model receives only text, not Mongo ids or byte ranges.
        List<String> texts = ragChunks.stream()
                .map(RagChunk::text)
                .toList();

        return embeddingPort.embedAll(texts)
                .flatMap(embeddings -> {
                    // The adapter returns embeddings in the same order as the input texts.
                    // That lets us pair ragChunks.get(i) with embeddings.get(i).
                    List<RagChunkVector> vectors = toVectors(ragChunks, embeddings);
                    long indexedChunkCount = ragChunks.size();

                    // The vector id is the same as the Mongo rag_chunk id.
                    // Later, search results can use that id to load the original text from MongoDB.
                    return vectorStore.saveAll(vectors)
                            .thenReturn(indexedChunkCount);
                });
    }

    private List<RagChunkVector> toVectors(List<RagChunk> ragChunks, List<TextEmbedding> embeddings) {
        if (embeddings.size() != ragChunks.size()) {
            throw new IllegalStateException(EMBEDDING_COUNT_DOES_NOT_MATCH_RAG_CHUNK_COUNT);
        }

        List<RagChunkVector> vectors = new ArrayList<>(ragChunks.size());

        for (int index = 0; index < ragChunks.size(); index++) {
            RagChunk ragChunk = ragChunks.get(index);
            TextEmbedding embedding = embeddings.get(index);

            vectors.add(new RagChunkVector(
                    ragChunk.id(),
                    ragChunk.datasetId(),
                    ragChunk.sourceChunkIndex(),
                    ragChunk.ragChunkIndex(),
                    embedding
            ));
        }

        return vectors;
    }
}
