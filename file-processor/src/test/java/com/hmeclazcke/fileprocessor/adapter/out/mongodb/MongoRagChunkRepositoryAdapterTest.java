package com.hmeclazcke.fileprocessor.adapter.out.mongodb;

import com.hmeclazcke.fileprocessor.domain.RagChunk;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoRagChunkRepositoryAdapterTest {

    private static final String DATASET_ID = "dataset-6g";

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoRagChunkRepositoryAdapter adapter = new MongoRagChunkRepositoryAdapter(mongoTemplate);

    @Test
    void savesRagChunksAsDocuments() {
        RagChunk first = new RagChunk(
                "dataset-6g:rag:2:0",
                DATASET_ID,
                2,
                0,
                "java reactor\n",
                100,
                113
        );
        RagChunk second = new RagChunk(
                "dataset-6g:rag:2:1",
                DATASET_ID,
                2,
                1,
                "mongo graphql\n",
                113,
                127
        );
        RagChunkDocument firstDocument = new RagChunkDocument(
                "dataset-6g:rag:2:0",
                DATASET_ID,
                2,
                0,
                "java reactor\n",
                100,
                113
        );
        RagChunkDocument secondDocument = new RagChunkDocument(
                "dataset-6g:rag:2:1",
                DATASET_ID,
                2,
                1,
                "mongo graphql\n",
                113,
                127
        );

        when(mongoTemplate.save(firstDocument)).thenReturn(Mono.just(firstDocument));
        when(mongoTemplate.save(secondDocument)).thenReturn(Mono.just(secondDocument));

        StepVerifier.create(adapter.saveAll(List.of(first, second)))
                .verifyComplete();

        verify(mongoTemplate).save(firstDocument);
        verify(mongoTemplate).save(secondDocument);
    }
}
