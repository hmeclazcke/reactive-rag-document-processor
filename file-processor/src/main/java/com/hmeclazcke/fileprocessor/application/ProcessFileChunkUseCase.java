package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.PartialWordCount;
import com.hmeclazcke.fileprocessor.domain.WordCounter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ProcessFileChunkUseCase {

    private final ChunkTextReaderPort textReader;
    private final WordCounter wordCounter;

    public ProcessFileChunkUseCase(ChunkTextReaderPort textReader, WordCounter wordCounter) {
        this.textReader = textReader;
        this.wordCounter = wordCounter;
    }

    public PartialWordCount process(Path datasetPath, FileChunk chunk) {
        List<String> textFragments = textReader.readText(datasetPath, chunk);
        Map<String, Long> wordCounts = wordCounter.count(textFragments);

        return new PartialWordCount(chunk.index(), wordCounts);
    }
}