package com.hmeclazcke.fileprocessor.adapter.out.mongodb;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public class MongoChunkWordCountRepositoryAdapter implements ChunkWordCountRepositoryPort {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoChunkWordCountRepositoryAdapter(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> save(ChunkWordCount chunkWordCount) {
        return Flux.fromIterable(chunkWordCount.wordCounts().entrySet())
                .map(entry -> toDocument(chunkWordCount.chunkIndex(), entry))
                // Store one Mongo document per word to avoid the 16 MB document size limit.
                .flatMap(mongoTemplate::save)
                // We do not need Mongo's saved document here; Mono<Void> only reports save success or failure.
                .then();
    }

    private ChunkWordCountDocument toDocument(int chunkIndex, Map.Entry<String, Long> wordCount) {
        return new ChunkWordCountDocument(
                documentId(chunkIndex, wordCount.getKey()),
                chunkIndex,
                wordCount.getKey(),
                wordCount.getValue()
        );
    }

    private String documentId(int chunkIndex, String word) {
        return chunkIndex + ":" + word;
    }
}