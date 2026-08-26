package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.application.port.out.DatasetQueryPort;
import com.hmeclazcke.filequeryapi.domain.Dataset;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

public class MongoDatasetQueryAdapter implements DatasetQueryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoDatasetQueryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<Dataset> findAllDatasets() {
        return mongoTemplate.findAll(DatasetDocument.class)
                .map(this::toDomain);
    }

    private Dataset toDomain(DatasetDocument document) {
        return new Dataset(
                document.id(),
                document.path(),
                document.fileSizeBytes(),
                document.chunkSizeBytes(),
                document.chunkCount()
        );
    }
}