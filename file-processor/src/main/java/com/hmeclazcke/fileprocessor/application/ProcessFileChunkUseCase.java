package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCounterPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ProcessFileChunkUseCase {

    private final ChunkWordCounterPort wordCounter;
    private final ChunkWordCountRepositoryPort repository;

    public ProcessFileChunkUseCase(ChunkWordCounterPort wordCounter, ChunkWordCountRepositoryPort repository) {
        this.wordCounter = wordCounter;
        this.repository = repository;
    }

    public Mono<ChunkWordCount> process(String datasetId, Path datasetPath, FileChunk chunk) {
        // Count the assigned chunk completely, then save that chunk result.
        return wordCounter.countWords(datasetPath, chunk)
                .map(wordCounts -> new ChunkWordCount(datasetId, chunk.index(), wordCounts))
                .flatMap(chunkWordCount -> repository.save(chunkWordCount)
                        .thenReturn(chunkWordCount));
    }
}
