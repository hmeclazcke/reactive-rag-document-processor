package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class FileSystemChunkWordCounterAdapterTest {

    @TempDir
    Path tempDir;

    private static final int MAX_LINE_LENGTH_BYTES = 1024 * 1024;
    private static final int REGULAR_TEST_BUFFER_SIZE_BYTES = 1024;
    private static final int SMALL_BUFFER_SIZE_BYTES = 5;
    private static final int TOO_SMALL_MAX_LINE_LENGTH_BYTES = 3;

    @Test
    void countsWordsFromOwnedLines() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\njava reactor\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectNext(Map.of(
                        "java", 2L,
                        "spring", 1L,
                        "reactor", 1L
                ))
                .verifyComplete();
    }

    @Test
    void countsCompleteLineWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, 7);

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectNext(Map.of(
                        "java", 1L,
                        "spring", 1L
                ))
                .verifyComplete();
    }

    @Test
    void skipsPartialLineWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(1, 7, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectNext(Map.of(
                        "reactor", 1L,
                        "mongo", 1L
                ))
                .verifyComplete();
    }

    @Test
    void countsOwnedLinesAfterSkippingPartialLineWithoutLosingBufferedBytes() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "first line\nsecond line\nthird line\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(1, 3, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
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

        FileSystemChunkWordCounterAdapter adapter = adapter(SMALL_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectNext(Map.of("reactivepipeline", 1L))
                .verifyComplete();
    }

    @Test
    void countsUtf8CharactersSplitAcrossBuffers() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "\u00c1baco ni\u00f1o\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = adapter(SMALL_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectNext(Map.of(
                        "\u00e1baco", 1L,
                        "ni\u00f1o", 1L
                ))
                .verifyComplete();
    }

    @Test
    void failsWhenLineExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java\n", StandardCharsets.UTF_8);

        FileSystemChunkWordCounterAdapter adapter = new FileSystemChunkWordCounterAdapter(
                new ChunkWordCounterSettings(TOO_SMALL_MAX_LINE_LENGTH_BYTES, REGULAR_TEST_BUFFER_SIZE_BYTES)
        );
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void failsWhenUtf8TextIsInvalid() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.write(datasetPath, new byte[]{(byte) 0xc3, 0x28, '\n'});

        FileSystemChunkWordCounterAdapter adapter = adapter(REGULAR_TEST_BUFFER_SIZE_BYTES);
        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.countWords(datasetPath, chunk))
                .expectError(IllegalStateException.class)
                .verify();
    }

    private FileSystemChunkWordCounterAdapter adapter(int bufferSizeBytes) {
        return new FileSystemChunkWordCounterAdapter(
                new ChunkWordCounterSettings(MAX_LINE_LENGTH_BYTES, bufferSizeBytes)
        );
    }
}
