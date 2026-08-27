package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.RagChunk;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RagChunkRepositoryPort {

    Mono<Void> saveAll(List<RagChunk> ragChunks);
}
