package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import reactor.core.publisher.Flux;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

public class MongoWordCountQueryAdapter implements WordCountQueryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoWordCountQueryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<WordCount> findTopWords(int limit) {
        Aggregation aggregation = newAggregation(
                group("word")
                        .first("word").as("word")
                        .sum("count").as("count"),
                sort(Sort.Direction.DESC, "count"),
                limit(limit)
        );

        return mongoTemplate.aggregate(
                        aggregation,
                        ChunkWordCountDocument.class,
                        WordCountAggregationResult.class
                )
                .map(result -> new WordCount(result.word(), result.count()));
    }
}