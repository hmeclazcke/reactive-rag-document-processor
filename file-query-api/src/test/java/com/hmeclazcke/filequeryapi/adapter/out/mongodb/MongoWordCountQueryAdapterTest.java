package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoWordCountQueryAdapterTest {

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoWordCountQueryAdapter adapter = new MongoWordCountQueryAdapter(mongoTemplate);

    @Test
    void findsTopWordsAcrossChunks() {
        when(mongoTemplate.aggregate(
                any(),
                eq(ChunkWordCountDocument.class),
                eq(WordCountAggregationResult.class)
        )).thenReturn(Flux.just(
                new WordCountAggregationResult("java", 5),
                new WordCountAggregationResult("reactor", 3)
        ));

        StepVerifier.create(adapter.findTopWords(2))
                .expectNext(
                        new WordCount("java", 5),
                        new WordCount("reactor", 3)
                )
                .verifyComplete();
    }
}