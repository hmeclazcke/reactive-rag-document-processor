package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.WordCounter;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ProcessFileChunkUseCase {

    private final ChunkTextReaderPort textReader;
    private final WordCounter wordCounter;
    private final ChunkWordCountRepositoryPort repository;

    public ProcessFileChunkUseCase(ChunkTextReaderPort textReader, WordCounter wordCounter, ChunkWordCountRepositoryPort repository) {
        this.textReader = textReader;
        this.wordCounter = wordCounter;
        this.repository = repository;
    }

    public Mono<ChunkWordCount> process(String datasetId, Path datasetPath, FileChunk chunk) {
        // Count the assigned chunk completely, then save that chunk result.
        return wordCounter.countReactive(textReader.readText(datasetPath, chunk))
                .map(wordCounts -> new ChunkWordCount(datasetId, chunk.index(), wordCounts))
                .flatMap(chunkWordCount -> repository.save(chunkWordCount)
                        .thenReturn(chunkWordCount));
    }
}