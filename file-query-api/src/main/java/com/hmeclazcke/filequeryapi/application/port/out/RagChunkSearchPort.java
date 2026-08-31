package com.hmeclazcke.filequeryapi.application.port.out;

import reactor.core.publisher.Flux;

public interface RagChunkSearchPort {

    Flux<String> findSimilarRagChunkIds(String datasetId, String question, int limit);
}
