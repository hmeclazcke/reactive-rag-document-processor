package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.domain.ChunkWordCountsComputed;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunkBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class FileSystemChunkProcessorAdapterTest {

    @TempDir
    Path tempDir;

    private static final String DATASET_ID = "dataset-6g";
    private static final int MAX_LINE_LENGTH_BYTES = 1024 * 1024;
    private static final int REGULAR_TEST_BUFFER_SIZE_BYTES = 1024;
    private static final int SMALL_BUFFER_SIZE_BYTES = 5;
    private static final int TOO_SMALL_MAX_LINE_LENGTH_BYTES = 3;
    private static final int REGULAR_RAG_CHUNK_MAX_TEXT_LENGTH_CHARACTERS = 20;
    private static final int REGULAR_RAG_CHUNK_BATCH_SIZE = 100;

    @Test
    void countsWordsFromOwnedLines() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\njava reactor\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of(
                        "java", 2L,
                        "spring", 1L,
                        "reactor", 1L
                ))
                .verifyComplete();
    }

    @Test
    void extractsRagChunksFromOwnedLines() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "alpha one\nbeta two\ngamma three\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(ragChunks(adapter, datasetPath, chunk))
                .expectNext(List.of(
                        new RagChunk(
                                "dataset-6g:rag:0:0",
                                DATASET_ID,
                                0,
                                0,
                                "alpha one\nbeta two\n",
                                0,
                                19
                        ),
                        new RagChunk(
                                "dataset-6g:rag:0:1",
                                DATASET_ID,
                                0,
                                1,
                                "gamma three\n",
                                19,
                                31
                        )
                ))
                .verifyComplete();
    }

    @Test
    void countsAndExtractsCompleteLineWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, 7);

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of(
                        "java", 1L,
                        "spring", 1L
                ))
                .verifyComplete();

        StepVerifier.create(ragChunks(adapter, datasetPath, chunk))
                .expectNext(List.of(
                        new RagChunk(
                                "dataset-6g:rag:0:0",
                                DATASET_ID,
                                0,
                                0,
                                "java spring\n",
                                0,
                                12
                        )
                ))
                .verifyComplete();
    }

    @Test
    void skipsPartialLineWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(1, 7, Files.size(datasetPath));

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of(
                        "reactor", 1L,
                        "mongo", 1L
                ))
                .verifyComplete();

        StepVerifier.create(ragChunks(adapter, datasetPath, chunk))
                .expectNext(List.of(
                        new RagChunk(
                                "dataset-6g:rag:1:0",
                                DATASET_ID,
                                1,
                                0,
                                "reactor mongo\n",
                                12,
                                26
                        )
                ))
                .verifyComplete();
    }

    @Test
    void countsOwnedLinesAfterSkippingPartialLineWithoutLosingBufferedBytes() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "first line\nsecond line\nthird line\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(1, 3, Files.size(datasetPath));

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of(
                        "second", 1L,
                        "line", 2L,
                        "third", 1L
                ))
                .verifyComplete();
    }

    @Test
    void countsWordsAcrossBufferBoundaries() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "reactivepipeline\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(SMALL_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of("reactivepipeline", 1L))
                .verifyComplete();
    }

    @Test
    void countsUtf8CharactersSplitAcrossBuffers() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "\u00c1baco ni\u00f1o\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(SMALL_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(wordCounts(adapter, datasetPath, chunk))
                .expectNext(Map.of(
                        "\u00e1baco", 1L,
                        "ni\u00f1o", 1L
                ))
                .verifyComplete();
    }

    @Test
    void extractsUtf8RagChunksSplitAcrossBuffers() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "\u00c1baco ni\u00f1o\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = adapter(SMALL_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(ragChunks(adapter, datasetPath, chunk))
                .expectNext(List.of(
                        new RagChunk(
                                "dataset-6g:rag:0:0",
                                DATASET_ID,
                                0,
                                0,
                                "\u00c1baco ni\u00f1o\n",
                                0,
                                Files.size(datasetPath)
                        )
                ))
                .verifyComplete();
    }

    @Test
    void emitsRagChunksInConfiguredBatches() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "a\nb\nc\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = new FileSystemChunkProcessorAdapter(
                new FileChunkProcessorSettings(
                        MAX_LINE_LENGTH_BYTES,
                        REGULAR_TEST_BUFFER_SIZE_BYTES,
                        2,
                        2
                )
        );
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(ragChunkBatchSizes(adapter, datasetPath, chunk))
                .expectNext(List.of(2, 1))
                .verifyComplete();
    }

    @Test
    void failsWhenLineExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java\n", StandardCharsets.UTF_8);

        FileSystemChunkProcessorAdapter adapter = new FileSystemChunkProcessorAdapter(
                new FileChunkProcessorSettings(
                        TOO_SMALL_MAX_LINE_LENGTH_BYTES,
                        REGULAR_TEST_BUFFER_SIZE_BYTES,
                        REGULAR_RAG_CHUNK_MAX_TEXT_LENGTH_CHARACTERS,
                        REGULAR_RAG_CHUNK_BATCH_SIZE
                )
        );
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.process(DATASET_ID, datasetPath, chunk))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void failsWhenUtf8TextIsInvalid() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.write(datasetPath, new byte[]{(byte) 0xc3, 0x28, '\n'});

        FileSystemChunkProcessorAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.process(DATASET_ID, datasetPath, chunk))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private FileSystemChunkProcessorAdapter adapter(int bufferSizeBytes) {
        return new FileSystemChunkProcessorAdapter(
                new FileChunkProcessorSettings(
                        MAX_LINE_LENGTH_BYTES,
                        bufferSizeBytes,
                        REGULAR_RAG_CHUNK_MAX_TEXT_LENGTH_CHARACTERS,
                        REGULAR_RAG_CHUNK_BATCH_SIZE
                )
        );
    }

    private Mono<Map<String, Long>> wordCounts(
            FileSystemChunkProcessorAdapter adapter,
            Path datasetPath,
            FileChunk chunk
    ) {
        return adapter.process(DATASET_ID, datasetPath, chunk)
                .ofType(ChunkWordCountsComputed.class)
                .single()
                .map(ChunkWordCountsComputed::wordCounts);
    }

    private Mono<List<RagChunk>> ragChunks(
            FileSystemChunkProcessorAdapter adapter,
            Path datasetPath,
            FileChunk chunk
    ) {
        return adapter.process(DATASET_ID, datasetPath, chunk)
                .ofType(RagChunkBatch.class)
                .flatMapIterable(RagChunkBatch::ragChunks)
                .collectList();
    }

    private Mono<List<Integer>> ragChunkBatchSizes(
            FileSystemChunkProcessorAdapter adapter,
            Path datasetPath,
            FileChunk chunk
    ) {
        return adapter.process(DATASET_ID, datasetPath, chunk)
                .ofType(RagChunkBatch.class)
                .map(RagChunkBatch::size)
                .collectList();
    }
}
