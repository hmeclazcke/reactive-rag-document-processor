package com.hmeclazcke.ragindexer.application.port.out;

import com.hmeclazcke.ragindexer.domain.RagChunk;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RagChunkIndexPort {

    Mono<Void> indexAll(List<RagChunk> ragChunks);
}
