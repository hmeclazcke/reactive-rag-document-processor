package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.WordCounter;
import com.hmeclazcke.fileprocessor.domain.WordTokenizer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.Map;

import static org.mockito.Mockito.*;

class ProcessFileChunkUseCaseTest {

    private static final String DATASET_ID = "dataset-6g";
    private final ChunkTextReaderPort textReader = mock(ChunkTextReaderPort.class);
    private final ChunkWordCountRepositoryPort repository = mock(ChunkWordCountRepositoryPort.class);
    private final WordTokenizer tokenizer = new WordTokenizer();
    private final WordCounter wordCounter = new WordCounter(tokenizer);
    private final ProcessFileChunkUseCase useCase = new ProcessFileChunkUseCase(textReader, wordCounter, repository);

    @Test
    void processesChunkLines() {
        Path datasetPath = Path.of("dataset.txt");
        FileChunk chunk = new FileChunk(2, 100, 200);
        ChunkWordCount expectedResult = new ChunkWordCount(
                DATASET_ID,
                chunk.index(),
                Map.of(
                        "java", 2L,
                        "reactor", 1L,
                        "mongo", 1L
                )
        );

        when(textReader.readText(datasetPath, chunk)).thenReturn(Flux.just(
                "java reactor",
                "java mongo"
        ));
        when(repository.save(expectedResult)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.process(DATASET_ID, datasetPath, chunk))
                .expectNext(expectedResult)
                .verifyComplete();

        verify(repository).save(expectedResult);
    }
}