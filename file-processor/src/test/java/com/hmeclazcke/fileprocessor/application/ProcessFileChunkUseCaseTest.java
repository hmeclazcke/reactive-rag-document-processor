package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.application.port.out.FileChunkProcessorPort;
import com.hmeclazcke.fileprocessor.application.port.out.RagChunkRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCountsComputed;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.ProcessedFileChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunkBatch;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessFileChunkUseCaseTest {

    private static final String DATASET_ID = "dataset-6g";

    private final FileChunkProcessorPort chunkProcessor = mock(FileChunkProcessorPort.class);
    private final ChunkWordCountRepositoryPort wordCountRepository = mock(ChunkWordCountRepositoryPort.class);
    private final RagChunkRepositoryPort ragChunkRepository = mock(RagChunkRepositoryPort.class);
    private final ProcessFileChunkUseCase useCase = new ProcessFileChunkUseCase(
            chunkProcessor,
            wordCountRepository,
            ragChunkRepository
    );

    @Test
    void processesChunkInOnePassAndSavesResults() {
        Path datasetPath = Path.of("dataset.txt");
        FileChunk chunk = new FileChunk(2, 100, 200);
        Map<String, Long> wordCounts = Map.of(
                "java", 2L,
                "reactor", 1L,
                "mongo", 1L
        );
        List<RagChunk> ragChunks = List.of(
                new RagChunk(
                        "dataset-6g:rag:2:0",
                        DATASET_ID,
                        chunk.index(),
                        0,
                        "java reactor\n",
                        100,
                        113
                ),
                new RagChunk(
                        "dataset-6g:rag:2:1",
                        DATASET_ID,
                        chunk.index(),
                        1,
                        "mongo\n",
                        114,
                        120
                )
        );
        ChunkWordCount expectedResult = new ChunkWordCount(
                DATASET_ID,
                chunk.index(),
                wordCounts
        );
        ProcessedFileChunk expectedProcessedFileChunk = new ProcessedFileChunk(
                expectedResult,
                ragChunks.size()
        );

        when(chunkProcessor.process(DATASET_ID, datasetPath, chunk)).thenReturn(Flux.just(
                new RagChunkBatch(ragChunks),
                new ChunkWordCountsComputed(wordCounts)
        ));
        when(ragChunkRepository.saveAll(ragChunks)).thenReturn(Mono.empty());
        when(wordCountRepository.save(expectedResult)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.process(DATASET_ID, datasetPath, chunk))
                .expectNext(expectedProcessedFileChunk)
                .verifyComplete();

        verify(chunkProcessor).process(DATASET_ID, datasetPath, chunk);
        verify(ragChunkRepository).saveAll(ragChunks);
        verify(wordCountRepository).save(expectedResult);
    }

    @Test
    void keepsProcessingStateIndependentForEachSubscription() {
        Path datasetPath = Path.of("dataset.txt");
        FileChunk chunk = new FileChunk(0, 0, 100);
        Map<String, Long> wordCounts = Map.of("reactor", 1L);
        List<RagChunk> ragChunks = List.of(
                new RagChunk(
                        "dataset-6g:rag:0:0",
                        DATASET_ID,
                        chunk.index(),
                        0,
                        "reactor\n",
                        0,
                        8
                )
        );
        ChunkWordCount expectedWordCount = new ChunkWordCount(
                DATASET_ID,
                chunk.index(),
                wordCounts
        );
        ProcessedFileChunk expectedResult = new ProcessedFileChunk(expectedWordCount, 1);

        when(chunkProcessor.process(DATASET_ID, datasetPath, chunk)).thenReturn(Flux.just(
                new RagChunkBatch(ragChunks),
                new ChunkWordCountsComputed(wordCounts)
        ));
        when(ragChunkRepository.saveAll(ragChunks)).thenReturn(Mono.empty());
        when(wordCountRepository.save(expectedWordCount)).thenReturn(Mono.empty());

        Mono<ProcessedFileChunk> processing = useCase.process(DATASET_ID, datasetPath, chunk);

        StepVerifier.create(processing)
                .expectNext(expectedResult)
                .verifyComplete();
        StepVerifier.create(processing)
                .expectNext(expectedResult)
                .verifyComplete();

        verify(chunkProcessor, times(2)).process(DATASET_ID, datasetPath, chunk);
        verify(ragChunkRepository, times(2)).saveAll(ragChunks);
        verify(wordCountRepository, times(2)).save(expectedWordCount);
    }
}
