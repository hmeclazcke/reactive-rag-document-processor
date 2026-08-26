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
                .map(entry -> toDocument(chunkWordCount.datasetId(), chunkWordCount.chunkIndex(), entry))
                // Store one Mongo document per word to avoid the 16 MB document size limit.
                .flatMap(mongoTemplate::save)
                // Discard Mongo's saved document and keep only save success or failure.
                .then();
    }

    private ChunkWordCountDocument toDocument(String datasetId, int chunkIndex, Map.Entry<String, Long> wordCount) {
        return new ChunkWordCountDocument(
                documentId(datasetId, chunkIndex, wordCount.getKey()),
                datasetId,
                chunkIndex,
                wordCount.getKey(),
                wordCount.getValue()
        );
    }

    private String documentId(String datasetId, int chunkIndex, String word) {
        return datasetId + ":" + chunkIndex + ":" + word;
    }
}