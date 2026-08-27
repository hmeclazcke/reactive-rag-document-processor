package com.hmeclazcke.fileprocessor.application;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.application.port.out.FileChunkProcessorPort;
import com.hmeclazcke.fileprocessor.application.port.out.RagChunkRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCountsComputed;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.FileChunkProcessingEvent;
import com.hmeclazcke.fileprocessor.domain.ProcessedFileChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunkBatch;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class ProcessFileChunkUseCase {

    private static final String CHUNK_PROCESSING_DID_NOT_EMIT_WORD_COUNTS =
            "chunk processing did not emit word counts";

    private final FileChunkProcessorPort chunkProcessor;
    private final ChunkWordCountRepositoryPort wordCountRepository;
    private final RagChunkRepositoryPort ragChunkRepository;

    public ProcessFileChunkUseCase(
            FileChunkProcessorPort chunkProcessor,
            ChunkWordCountRepositoryPort wordCountRepository,
            RagChunkRepositoryPort ragChunkRepository
    ) {
        this.chunkProcessor = chunkProcessor;
        this.wordCountRepository = wordCountRepository;
        this.ragChunkRepository = ragChunkRepository;
    }

    public Mono<ProcessedFileChunk> process(String datasetId, Path datasetPath, FileChunk chunk) {
        ProcessingState state = new ProcessingState();

        return chunkProcessor.process(datasetId, datasetPath, chunk)
                // The adapter scans the file once. RAG batches are persisted as they are emitted;
                // word counts are emitted once at the end because they need the full chunk result.
                .concatMap(event -> persist(event, datasetId, chunk, state), 1)
                .then(Mono.fromSupplier(state::processedFileChunk));
    }

    private Mono<Void> persist(
            FileChunkProcessingEvent event,
            String datasetId,
            FileChunk chunk,
            ProcessingState state
    ) {
        return switch (event) {
            case RagChunkBatch ragChunkBatch -> {
                state.addRagChunks(ragChunkBatch.size());
                yield ragChunkRepository.saveAll(ragChunkBatch.ragChunks());
            }
            case ChunkWordCountsComputed wordCountsComputed -> saveWordCounts(datasetId, chunk, state, wordCountsComputed);
        };
    }

    private Mono<Void> saveWordCounts(
            String datasetId,
            FileChunk chunk,
            ProcessingState state,
            ChunkWordCountsComputed wordCountsComputed
    ) {
        ChunkWordCount chunkWordCount = new ChunkWordCount(
                datasetId,
                chunk.index(),
                wordCountsComputed.wordCounts()
        );

        state.setChunkWordCount(chunkWordCount);
        return wordCountRepository.save(chunkWordCount);
    }

    private static final class ProcessingState {

        private ChunkWordCount chunkWordCount;
        private long ragChunkCount;

        private void addRagChunks(long count) {
            ragChunkCount += count;
        }

        private void setChunkWordCount(ChunkWordCount chunkWordCount) {
            this.chunkWordCount = chunkWordCount;
        }

        private ProcessedFileChunk processedFileChunk() {
            if (chunkWordCount == null) {
                throw new IllegalStateException(CHUNK_PROCESSING_DID_NOT_EMIT_WORD_COUNTS);
            }

            return new ProcessedFileChunk(chunkWordCount, ragChunkCount);
        }
    }
}
