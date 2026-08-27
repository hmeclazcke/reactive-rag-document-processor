package com.hmeclazcke.ragindexer.application.port.out;

import com.hmeclazcke.ragindexer.domain.RagChunkVector;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RagChunkVectorStorePort {

    Mono<Void> saveAll(List<RagChunkVector> vectors);
}
