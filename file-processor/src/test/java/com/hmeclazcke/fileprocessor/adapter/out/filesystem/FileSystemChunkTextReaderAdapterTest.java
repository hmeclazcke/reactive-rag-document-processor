package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class FileSystemChunkTextReaderAdapterTest {

    @TempDir
    Path tempDir;

    private static final int MAX_LINE_LENGTH_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE_BYTES = 64 * 1024;
    private static final int TOO_SMALL_MAX_LINE_LENGTH_BYTES = 3;

    @Test
    void readsCompleteLineWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_LINE_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, 7);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java spring\n")
                .verifyComplete();
    }

    @Test
    void skipsPartialLineWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring\nreactor mongo\n", StandardCharsets.UTF_8);

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_LINE_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(1, 7, Files.size(datasetPath));

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("reactor mongo\n")
                .verifyComplete();
    }

    @Test
    void readsUtf8CharactersWithoutCorruptingThem() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "niño café\n", StandardCharsets.UTF_8);

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_LINE_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("niño café\n")
                .verifyComplete();
    }

    @Test
    void emitsMultipleLinesWhenChunkOwnsMultipleLineStarts() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java\nspring\nreactor\n", StandardCharsets.UTF_8);

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_LINE_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, 13);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java\n")
                .expectNext("spring\n")
                .expectNext("reactor\n")
                .verifyComplete();
    }

    @Test
    void failsWhenLineExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java\n", StandardCharsets.UTF_8);

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(TOO_SMALL_MAX_LINE_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, Files.size(datasetPath));

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectError(IllegalStateException.class)
                .verify();
    }
}