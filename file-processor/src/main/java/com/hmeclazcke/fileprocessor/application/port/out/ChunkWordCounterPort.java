package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Map;

public interface ChunkWordCounterPort {

    Mono<Map<String, Long>> countWords(Path datasetPath, FileChunk chunk);
}
