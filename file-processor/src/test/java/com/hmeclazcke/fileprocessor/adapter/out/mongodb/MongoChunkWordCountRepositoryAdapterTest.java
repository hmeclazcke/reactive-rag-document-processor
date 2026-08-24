package com.hmeclazcke.fileprocessor.adapter.out.mongodb;

import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoChunkWordCountRepositoryAdapterTest {

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoChunkWordCountRepositoryAdapter adapter = new MongoChunkWordCountRepositoryAdapter(mongoTemplate);

    @Test
    void savesOneDocumentPerWordCount() {
        ChunkWordCount chunkWordCount = new ChunkWordCount(
                2,
                Map.of(
                        "java", 3L,
                        "reactor", 1L
                )
        );
        ChunkWordCountDocument javaDocument = new ChunkWordCountDocument(
                "2:java",
                2,
                "java",
                3L
        );
        ChunkWordCountDocument reactorDocument = new ChunkWordCountDocument(
                "2:reactor",
                2,
                "reactor",
                1L
        );

        when(mongoTemplate.save(javaDocument)).thenReturn(Mono.just(javaDocument));
        when(mongoTemplate.save(reactorDocument)).thenReturn(Mono.just(reactorDocument));

        StepVerifier.create(adapter.save(chunkWordCount))
                .verifyComplete();

        verify(mongoTemplate).save(javaDocument);
        verify(mongoTemplate).save(reactorDocument);
    }
}