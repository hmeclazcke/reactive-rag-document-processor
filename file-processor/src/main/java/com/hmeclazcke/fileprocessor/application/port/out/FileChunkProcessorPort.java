package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.FileChunkProcessingEvent;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public interface FileChunkProcessorPort {

    Flux<FileChunkProcessingEvent> process(String datasetId, Path datasetPath, FileChunk chunk);
}
