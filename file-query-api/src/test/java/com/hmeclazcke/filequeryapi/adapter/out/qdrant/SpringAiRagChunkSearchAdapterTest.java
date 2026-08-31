package com.hmeclazcke.filequeryapi.adapter.out.qdrant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiRagChunkSearchAdapterTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final String QUESTION = "How does the processor create RAG chunks?";
    private static final int LIMIT = 2;

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final SpringAiRagChunkSearchAdapter adapter =
            new SpringAiRagChunkSearchAdapter(vectorStore);

    @Test
    void returnsRagChunkIdsFromSimilaritySearchInVectorStoreOrder() {
        Document first = new Document(
                "document-1",
                "first source text",
                Map.of("ragChunkId", "dataset-1g-gemini:rag:0:1")
        );
        Document second = new Document(
                "document-2",
                "second source text",
                Map.of("ragChunkId", "dataset-1g-gemini:rag:0:2")
        );

        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of(first, second));

        StepVerifier.create(adapter.findSimilarRagChunkIds(DATASET_ID, QUESTION, LIMIT))
                .expectNext(
                        "dataset-1g-gemini:rag:0:1",
                        "dataset-1g-gemini:rag:0:2"
                )
                .verifyComplete();

        ArgumentCaptor<SearchRequest> searchRequestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());

        SearchRequest searchRequest = searchRequestCaptor.getValue();
        assertEquals(QUESTION, searchRequest.getQuery());
        assertEquals(LIMIT, searchRequest.getTopK());
        assertTrue(searchRequest.hasFilterExpression());
    }

    @Test
    void ignoresDocumentsWithoutRagChunkIdMetadata() {
        Document documentWithoutRagChunkId = new Document(
                "document-1",
                "source text",
                Map.of("datasetId", DATASET_ID)
        );

        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of(documentWithoutRagChunkId));

        StepVerifier.create(adapter.findSimilarRagChunkIds(DATASET_ID, QUESTION, LIMIT))
                .verifyComplete();
    }
}
