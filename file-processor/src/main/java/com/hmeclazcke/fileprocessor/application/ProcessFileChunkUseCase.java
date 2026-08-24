package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.PartialWordCount;
import com.hmeclazcke.fileprocessor.domain.WordCounter;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ProcessFileChunkUseCase {

    private final ChunkTextReaderPort textReader;
    private final WordCounter wordCounter;

    public ProcessFileChunkUseCase(ChunkTextReaderPort textReader, WordCounter wordCounter) {
        this.textReader = textReader;
        this.wordCounter = wordCounter;
    }

    public Mono<PartialWordCount> process(Path datasetPath, FileChunk chunk) {
        return wordCounter.countReactive(textReader.readText(datasetPath, chunk))
                .map(wordCounts -> new PartialWordCount(chunk.index(), wordCounts));
    }
}