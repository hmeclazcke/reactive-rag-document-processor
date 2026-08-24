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

    @Test
    void completesWordWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(0, 0, 7);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java spring")
                .verifyComplete();
    }

    @Test
    void discardsWordFragmentWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 7, 15);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("reactor")
                .verifyComplete();
    }

    @Test
    void failsWhenWordExtensionExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java verylongword");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(3);

        FileChunk chunk = new FileChunk(0, 0, 9);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectError(WordTooLongException.class)
                .verify();
    }

    @Test
    void failsWhenSkippedWordFragmentExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "verylongword java");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(3);

        FileChunk chunk = new FileChunk(1, 4, 12);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectError(WordTooLongException.class)
                .verify();
    }

    @Test
    void keepsWordWhenChunkStartsAtSeparatorBeforeIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 4, 11);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext(" spring")
                .verifyComplete();
    }

    @Test
    void keepsWordWhenChunkStartsAtWordStart() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 5, 11);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("spring")
                .verifyComplete();
    }

    @Test
    void doesNotReadNextWordWhenChunkEndsAtSeparator() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(0, 0, 5);

        StepVerifier.create(adapter.readText(datasetPath, chunk))
                .expectNext("java ")
                .verifyComplete();
    }
}