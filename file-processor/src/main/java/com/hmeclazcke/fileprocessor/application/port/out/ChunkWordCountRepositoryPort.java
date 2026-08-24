package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import reactor.core.publisher.Mono;

public interface ChunkWordCountRepositoryPort {

    Mono<Void> save(ChunkWordCount chunkWordCount);
}