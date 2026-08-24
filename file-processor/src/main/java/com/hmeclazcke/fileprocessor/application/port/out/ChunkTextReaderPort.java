package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public interface ChunkTextReaderPort {

    Flux<String> readText(Path datasetPath, FileChunk chunk);
}