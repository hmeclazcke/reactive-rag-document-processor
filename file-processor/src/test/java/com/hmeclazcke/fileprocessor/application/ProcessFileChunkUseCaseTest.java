package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.PartialWordCount;
import com.hmeclazcke.fileprocessor.domain.WordCounter;
import com.hmeclazcke.fileprocessor.domain.WordTokenizer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessFileChunkUseCaseTest {

    private final ChunkTextReaderPort textReader = mock(ChunkTextReaderPort.class);
    private final WordTokenizer tokenizer = new WordTokenizer();
    private final WordCounter wordCounter = new WordCounter(tokenizer);
    private final ProcessFileChunkUseCase useCase = new ProcessFileChunkUseCase(textReader, wordCounter);

    @Test
    void processesChunkLines() {
        Path datasetPath = Path.of("dataset.txt");
        FileChunk chunk = new FileChunk(2, 100, 200);

        when(textReader.readText(datasetPath, chunk)).thenReturn(List.of(
                "java reactor",
                "java mongo"
        ));

        PartialWordCount result = useCase.process(datasetPath, chunk);

        assertEquals(new PartialWordCount(
                chunk.index(),
                Map.of(
                        "java", 2L,
                        "reactor", 1L,
                        "mongo", 1L
                )
        ), result);
    }
}