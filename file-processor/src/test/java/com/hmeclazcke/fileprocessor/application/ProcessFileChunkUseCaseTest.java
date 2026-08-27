package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCounterPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessFileChunkUseCaseTest {

    private static final String DATASET_ID = "dataset-6g";

    private final ChunkWordCounterPort wordCounter = mock(ChunkWordCounterPort.class);
    private final ChunkWordCountRepositoryPort repository = mock(ChunkWordCountRepositoryPort.class);
    private final ProcessFileChunkUseCase useCase = new ProcessFileChunkUseCase(wordCounter, repository);

    @Test
    void processesChunkWordCounts() {
        Path datasetPath = Path.of("dataset.txt");
        FileChunk chunk = new FileChunk(2, 100, 200);
        Map<String, Long> wordCounts = Map.of(
                "java", 2L,
                "reactor", 1L,
                "mongo", 1L
        );
        ChunkWordCount expectedResult = new ChunkWordCount(
                DATASET_ID,
                chunk.index(),
                wordCounts
        );

        when(wordCounter.countWords(datasetPath, chunk)).thenReturn(Mono.just(wordCounts));
        when(repository.save(expectedResult)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.process(DATASET_ID, datasetPath, chunk))
                .expectNext(expectedResult)
                .verifyComplete();

        verify(wordCounter).countWords(datasetPath, chunk);
        verify(repository).save(expectedResult);
    }
}
