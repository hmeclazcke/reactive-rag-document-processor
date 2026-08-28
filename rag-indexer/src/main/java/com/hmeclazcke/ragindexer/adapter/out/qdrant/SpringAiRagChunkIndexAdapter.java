package com.hmeclazcke.ragindexer.adapter.out.qdrant;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkIndexPort;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpringAiRagChunkIndexAdapter implements RagChunkIndexPort {

    private static final String DOCUMENT_ID_NAMESPACE = "rag-chunk:";

    private final VectorStore vectorStore;

    public SpringAiRagChunkIndexAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Mono<Void> indexAll(List<RagChunk> ragChunks) {
        if (ragChunks.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> vectorStore.add(toDocuments(ragChunks)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private List<Document> toDocuments(List<RagChunk> ragChunks) {
        return ragChunks.stream()
                .map(this::toDocument)
                .toList();
    }

    private Document toDocument(RagChunk ragChunk) {
        return new Document(
                toDocumentId(ragChunk.id()),
                ragChunk.text(),
                Map.of(
                        "ragChunkId", ragChunk.id(),
                        "datasetId", ragChunk.datasetId(),
                        "sourceChunkIndex", ragChunk.sourceChunkIndex(),
                        "ragChunkIndex", ragChunk.ragChunkIndex()
                )
        );
    }

    private String toDocumentId(String ragChunkId) {
        byte[] source = (DOCUMENT_ID_NAMESPACE + ragChunkId).getBytes(StandardCharsets.UTF_8);

        return UUID.nameUUIDFromBytes(source).toString();
    }
}
