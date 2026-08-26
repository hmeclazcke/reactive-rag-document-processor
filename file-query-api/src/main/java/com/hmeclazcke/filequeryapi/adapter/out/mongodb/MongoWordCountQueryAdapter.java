package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Flux;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

public class MongoWordCountQueryAdapter implements WordCountQueryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoWordCountQueryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<WordCount> findTopWords(String datasetId, int limit) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("datasetId").is(datasetId)),
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