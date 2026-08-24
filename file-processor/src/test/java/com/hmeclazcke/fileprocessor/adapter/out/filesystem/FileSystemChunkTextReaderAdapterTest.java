package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.WordTooLongException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;

class FileSystemChunkTextReaderAdapterTest {

    @TempDir
    Path tempDir;
    private static final int MAX_WORD_LENGTH_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE_BYTES = 64 * 1024;
    private static final int TOO_SMALL_MAX_WORD_LENGTH_BYTES = 3;

    @Test
    void completesWordWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, 7);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java spring")
                .verifyComplete();
    }

    @Test
    void discardsWordFragmentWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(1, 7, 15);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("reactor")
                .verifyComplete();
    }

    @Test
    void failsWhenWordExtensionExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java verylongword");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(TOO_SMALL_MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, 9);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectError(WordTooLongException.class)
                .verify();
    }

    @Test
    void failsWhenSkippedWordFragmentExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "verylongword java");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(TOO_SMALL_MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(1, 4, 12);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectError(WordTooLongException.class)
                .verify();
    }

    @Test
    void keepsWordWhenChunkStartsAtSeparatorBeforeIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(1, 4, 11);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext(" spring")
                .verifyComplete();
    }

    @Test
    void keepsWordWhenChunkStartsAtWordStart() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(1, 5, 11);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("spring")
                .verifyComplete();
    }

    @Test
    void doesNotReadNextWordWhenChunkEndsAtSeparator() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, BUFFER_SIZE_BYTES)
        );

        FileChunk chunk = new FileChunk(0, 0, 5);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java ")
                .verifyComplete();
    }

    @Test
    void emitsMultipleTextFragmentsWhenBufferIsSmallerThanChunk() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor mongo");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(
                new ChunkTextReaderSettings(MAX_WORD_LENGTH_BYTES, 12)
        );

        FileChunk chunk = new FileChunk(0, 0, 25);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java spring ")
                .expectNext("reactor ")
                .expectNext("mongo")
                .verifyComplete();
    }
}