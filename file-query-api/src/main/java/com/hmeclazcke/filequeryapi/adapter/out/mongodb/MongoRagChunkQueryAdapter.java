package com.hmeclazcke.filequeryapi.adapter.out.mongodb;

import com.hmeclazcke.filequeryapi.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.filequeryapi.domain.RagChunk;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

import java.util.List;

public class MongoRagChunkQueryAdapter implements RagChunkQueryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoRagChunkQueryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<RagChunk> findByDatasetIdAndIds(String datasetId, List<String> ragChunkIds) {
        if (ragChunkIds.isEmpty()) {
            // No ids from retrieval means there is no source text to recover.
            return Flux.empty();
        }

        // Retrieval gives us stable RAG chunk ids.
        // MongoDB is the source of truth for the original text and byte offsets for those ids.
        Query query = Query.query(
                Criteria.where("datasetId").is(datasetId)
                        .and("_id").in(ragChunkIds)
        );

        // This adapter only recovers source chunks.
        // The use case keeps the final context ordered by similarity score.
        return mongoTemplate.find(query, RagChunkDocument.class)
                .map(this::toDomain);
    }

    private RagChunk toDomain(RagChunkDocument document) {
        // Keep the Mongo document shape at the adapter boundary.
        // The rest of the application works with the domain RagChunk record.
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
