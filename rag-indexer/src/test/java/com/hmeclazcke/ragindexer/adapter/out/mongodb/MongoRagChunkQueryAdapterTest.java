package com.hmeclazcke.ragindexer.adapter.out.mongodb;

import com.hmeclazcke.ragindexer.domain.RagChunk;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoRagChunkQueryAdapterTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final int BATCH_SIZE = 100;

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoRagChunkQueryAdapter adapter = new MongoRagChunkQueryAdapter(mongoTemplate);

    @Test
    void findsRagChunksByDatasetId() {
        RagChunkDocument document = new RagChunkDocument(
                "dataset-1g-gemini:rag:0:1",
                DATASET_ID,
                0,
                1,
                "java reactor\n",
                100,
                113
        );

        when(mongoTemplate.find(any(Query.class), eq(RagChunkDocument.class)))
                .thenReturn(Flux.just(document));

        StepVerifier.create(adapter.findByDatasetId(DATASET_ID, BATCH_SIZE))
                .expectNext(new RagChunk(
                        "dataset-1g-gemini:rag:0:1",
                        DATASET_ID,
                        0,
                        1,
                        "java reactor\n",
                        100,
                        113
                ))
                .verifyComplete();
    }
}
