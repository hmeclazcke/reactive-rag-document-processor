package com.hmeclazcke.fileprocessor.adapter.out.mongodb;

import com.hmeclazcke.fileprocessor.application.port.out.RagChunkRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.RagChunk;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class MongoRagChunkRepositoryAdapter implements RagChunkRepositoryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoRagChunkRepositoryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> saveAll(List<RagChunk> ragChunks) {
        return Flux.fromIterable(ragChunks)
                .map(this::toDocument)
                .flatMap(mongoTemplate::save)
                .then();
    }

    private RagChunkDocument toDocument(RagChunk ragChunk) {
        return new RagChunkDocument(
                ragChunk.id(),
                ragChunk.datasetId(),
                ragChunk.sourceChunkIndex(),
                ragChunk.ragChunkIndex(),
                ragChunk.text(),
                ragChunk.startByteInclusive(),
                ragChunk.endByteExclusive()
        );
    }
}
