package com.hmeclazcke.filequeryapi.adapter.out.qdrant;

import com.hmeclazcke.filequeryapi.application.port.out.RagChunkSearchPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class SpringAiRagChunkSearchAdapter implements RagChunkSearchPort {

    // These metadata keys are written by rag-indexer when it stores each RAG chunk in Qdrant.
    // Query-side retrieval uses them to stay inside one dataset and to get back to MongoDB text.
    private static final String DATASET_ID_METADATA_KEY = "datasetId";
    private static final String RAG_CHUNK_ID_METADATA_KEY = "ragChunkId";

    private final VectorStore vectorStore;

    public SpringAiRagChunkSearchAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Flux<String> findSimilarRagChunkIds(String datasetId, String question, int limit) {
        // SearchRequest is the retrieval request: "embed this question and find the closest chunks".
        // Spring AI handles the question embedding before it asks Qdrant for similar vectors.
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(limit)
                .filterExpression(datasetFilter(datasetId))
                .build();

        // Mono does not make this call non-blocking by itself.
        // Spring AI exposes similaritySearch as a synchronous call, so it runs on boundedElastic.
        return Mono.fromSupplier(() -> vectorStore.similaritySearch(searchRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .handle((document, sink) -> {
                    String ragChunkId = ragChunkIdFrom(document);

                    if (ragChunkId != null) {
                        sink.next(ragChunkId);
                    }
                });
    }

    private Filter.Expression datasetFilter(String datasetId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        // Filter.Expression is Spring AI's portable filter object.
        // In plain terms, this is the vector-search version of: WHERE datasetId = ?
        return builder.eq(DATASET_ID_METADATA_KEY, datasetId).build();
    }

    private String ragChunkIdFrom(Document document) {
        // Qdrant returns Spring AI Documents with metadata.
        // The stable ragChunkId is the bridge back to the canonical source text in MongoDB.
        Object ragChunkId = document.getMetadata().get(RAG_CHUNK_ID_METADATA_KEY);

        if (ragChunkId == null) {
            return null;
        }

        return ragChunkId.toString();
    }
}
