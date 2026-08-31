package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.filequeryapi.application.port.out.RagChunkSearchPort;
import com.hmeclazcke.filequeryapi.domain.RagChunk;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchDocumentContextUseCase {

    private static final String DATASET_ID_VALIDATION_MESSAGE =
            "datasetId must not be blank";

    private static final String QUESTION_VALIDATION_MESSAGE =
            "question must not be blank";

    private static final String RETRIEVED_CHUNK_LIMIT_VALIDATION_MESSAGE =
            "retrievedChunkLimit must be greater than zero";

    private final RagChunkSearchPort ragChunkSearch;
    private final RagChunkQueryPort ragChunkQuery;
    private final int retrievedChunkLimit;

    public SearchDocumentContextUseCase(
            RagChunkSearchPort ragChunkSearch,
            RagChunkQueryPort ragChunkQuery,
            int retrievedChunkLimit
    ) {
        if (retrievedChunkLimit <= 0) {
            throw new IllegalArgumentException(RETRIEVED_CHUNK_LIMIT_VALIDATION_MESSAGE);
        }

        this.ragChunkSearch = ragChunkSearch;
        this.ragChunkQuery = ragChunkQuery;
        this.retrievedChunkLimit = retrievedChunkLimit;
    }

    public Mono<List<RagChunkSource>> search(String datasetId, String question) {
        if (datasetId == null || datasetId.isBlank()) {
            return Mono.error(new IllegalArgumentException(DATASET_ID_VALIDATION_MESSAGE));
        }

        if (question == null || question.isBlank()) {
            return Mono.error(new IllegalArgumentException(QUESTION_VALIDATION_MESSAGE));
        }

        // Retrieval returns ids in relevance order; MongoDB then recovers the canonical text for those ids.
        return ragChunkSearch.findSimilarRagChunkIds(datasetId, question, retrievedChunkLimit)
                .collectList()
                .flatMap(ragChunkIds -> recoverSources(datasetId, ragChunkIds));
    }

    private Mono<List<RagChunkSource>> recoverSources(String datasetId, List<String> ragChunkIds) {
        if (ragChunkIds.isEmpty()) {
            return Mono.just(List.of());
        }

        return ragChunkQuery.findByDatasetIdAndIds(datasetId, ragChunkIds)
                .collectList()
                .map(ragChunks -> orderBySimilaritySearch(ragChunkIds, ragChunks))
                .map(this::sourcesFrom);
    }

    private List<RagChunkSource> sourcesFrom(List<RagChunk> contextChunks) {
        return IntStream.range(0, contextChunks.size())
                .mapToObj(index -> sourceFrom(index, contextChunks.get(index)))
                .toList();
    }

    private RagChunkSource sourceFrom(int index, RagChunk chunk) {
        return new RagChunkSource(
                index + 1,
                chunk.id(),
                chunk.sourceChunkIndex(),
                chunk.ragChunkIndex(),
                chunk.startByteInclusive(),
                chunk.endByteExclusive(),
                chunk.text()
        );
    }

    private List<RagChunk> orderBySimilaritySearch(
            List<String> ragChunkIds,
            List<RagChunk> ragChunks
    ) {
        Map<String, RagChunk> chunksById = ragChunks.stream()
                .collect(Collectors.toMap(RagChunk::id, Function.identity()));

        return ragChunkIds.stream()
                .map(chunksById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
