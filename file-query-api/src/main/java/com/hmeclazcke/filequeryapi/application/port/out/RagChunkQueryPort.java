package com.hmeclazcke.filequeryapi.application.port.out;

import com.hmeclazcke.filequeryapi.domain.RagChunk;
import reactor.core.publisher.Flux;

import java.util.List;

public interface RagChunkQueryPort {

    Flux<RagChunk> findByDatasetIdAndIds(String datasetId, List<String> ragChunkIds);
}
