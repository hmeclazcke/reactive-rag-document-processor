package com.hmeclazcke.filecoordinator.adapter.out.mongodb;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetMetadataRepositoryPort;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

public class MongoDatasetMetadataRepositoryAdapter implements DatasetMetadataRepositoryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoDatasetMetadataRepositoryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> save(ProcessingPlan plan) {
        return mongoTemplate.save(toDocument(plan)).then();
    }

    private DatasetMetadataDocument toDocument(ProcessingPlan plan) {
        return new DatasetMetadataDocument(
                plan.datasetId(),
                plan.datasetPath().toString(),
                plan.fileSizeBytes(),
                plan.chunkSizeBytes(),
                plan.chunks().size()
        );
    }
}