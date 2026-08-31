package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.domain.RagChunk;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MongoRagChunkQueryAdapterTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final String FIRST_ID = "dataset-1g-gemini:rag:0:1";
    private static final String SECOND_ID = "dataset-1g-gemini:rag:0:2";

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoRagChunkQueryAdapter adapter = new MongoRagChunkQueryAdapter(mongoTemplate);

    @Test
    void findsRagChunksByDatasetAndIds() {
        List<String> ragChunkIds = List.of(FIRST_ID, SECOND_ID);

        when(mongoTemplate.find(any(Query.class), eq(RagChunkDocument.class)))
                .thenReturn(Flux.just(
                        new RagChunkDocument(
                                FIRST_ID,
                                DATASET_ID,
                                0,
                                1,
                                "The processor accumulates complete lines.",
                                100,
                                150
                        ),
                        new RagChunkDocument(
                                SECOND_ID,
                                DATASET_ID,
                                0,
                                2,
                                "RAG chunks are persisted in MongoDB.",
                                151,
                                200
                        )
                ));

        StepVerifier.create(adapter.findByDatasetIdAndIds(DATASET_ID, ragChunkIds))
                .expectNext(
                        new RagChunk(
                                FIRST_ID,
                                DATASET_ID,
                                0,
                                1,
                                "The processor accumulates complete lines.",
                                100,
                                150
                        ),
                        new RagChunk(
                                SECOND_ID,
                                DATASET_ID,
                                0,
                                2,
                                "RAG chunks are persisted in MongoDB.",
                                151,
                                200
                        )
                )
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenThereAreNoIdsToFind() {
        StepVerifier.create(adapter.findByDatasetIdAndIds(DATASET_ID, List.of()))
                .verifyComplete();

        verifyNoInteractions(mongoTemplate);
    }
}
