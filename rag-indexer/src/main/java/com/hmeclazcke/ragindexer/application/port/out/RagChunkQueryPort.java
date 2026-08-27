package com.hmeclazcke.ragindexer.application.port.out;

import com.hmeclazcke.ragindexer.domain.RagChunk;
import reactor.core.publisher.Flux;

public interface RagChunkQueryPort {

    Flux<RagChunk> findByDatasetId(String datasetId, int batchSize);
}
