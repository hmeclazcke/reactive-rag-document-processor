package com.hmeclazcke.ragindexer.adapter.out.qdrant;

import com.hmeclazcke.ragindexer.domain.RagChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SpringAiRagChunkIndexAdapterTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final SpringAiRagChunkIndexAdapter adapter =
            new SpringAiRagChunkIndexAdapter(vectorStore);

    @Test
    void addsRagChunksAsSpringAiDocuments() {
        RagChunk ragChunk = new RagChunk(
                "dataset-1g-gemini:rag:0:1",
                "dataset-1g-gemini",
                0,
                1,
                "java reactor qdrant",
                100,
                120
        );

        StepVerifier.create(adapter.indexAll(List.of(ragChunk)))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());

        Document document = documentsCaptor.getValue().getFirst();
        String expectedDocumentId = UUID.nameUUIDFromBytes(
                "rag-chunk:dataset-1g-gemini:rag:0:1".getBytes(StandardCharsets.UTF_8)
        ).toString();

        assertEquals(expectedDocumentId, document.getId());
        assertEquals(ragChunk.text(), document.getText());
        assertEquals(ragChunk.id(), document.getMetadata().get("ragChunkId"));
        assertEquals(ragChunk.datasetId(), document.getMetadata().get("datasetId"));
        assertEquals(ragChunk.sourceChunkIndex(), document.getMetadata().get("sourceChunkIndex"));
        assertEquals(ragChunk.ragChunkIndex(), document.getMetadata().get("ragChunkIndex"));
    }

    @Test
    void doesNothingWhenThereAreNoRagChunks() {
        StepVerifier.create(adapter.indexAll(List.of()))
                .verifyComplete();

        verifyNoInteractions(vectorStore);
    }
}
