package com.hmeclazcke.ragindexer.adapter.out.mongodb;

import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.ragindexer.domain.RagChunk;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class MongoRagChunkQueryAdapter implements RagChunkQueryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoRagChunkQueryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RagChunk> findByDatasetId(String datasetId, int batchSize) {
        Query query = Query.query(Criteria.where("datasetId").is(datasetId))
                .with(Sort.by(
                        Sort.Order.asc("sourceChunkIndex"),
                        Sort.Order.asc("ragChunkIndex")
                ))
                .cursorBatchSize(batchSize);

        return mongoTemplate.find(query, RagChunkDocument.class)
                .map(this::toDomain);
    }

    private RagChunk toDomain(RagChunkDocument document) {
        return new RagChunk(
                document.id(),
                document.datasetId(),
                document.sourceChunkIndex(),
                document.ragChunkIndex(),
                document.text(),
                document.startByteInclusive(),
                document.endByteExclusive()
        );
    }
}
