package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.domain.FileChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemChunkTextReaderAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void completesWordWhenChunkEndsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(0, 0, 7);

        List<String> textFragments = adapter.readText(datasetPath, chunk);

        assertEquals(List.of("java spring"), textFragments);
    }

    @Test
    void discardsWordFragmentWhenChunkStartsInTheMiddleOfIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring reactor");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 7, 15);

        List<String> textFragments = adapter.readText(datasetPath, chunk);

        assertEquals(List.of("reactor"), textFragments);
    }

    @Test
    void failsWhenWordExtensionExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java verylongword");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(3);

        FileChunk chunk = new FileChunk(0, 0, 9);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.readText(datasetPath, chunk)
        );

        assertEquals("Word exceeds maximum supported length", exception.getMessage());
    }

    @Test
    void failsWhenSkippedWordFragmentExceedsLimit() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "verylongword java");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter(3);

        FileChunk chunk = new FileChunk(1, 4, 12);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.readText(datasetPath, chunk)
        );

        assertEquals("Word exceeds maximum supported length", exception.getMessage());
    }

    @Test
    void keepsWordWhenChunkStartsAtSeparatorBeforeIt() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 4, 11);

        List<String> textFragments = adapter.readText(datasetPath, chunk);

        assertEquals(List.of(" spring"), textFragments);
    }

    @Test
    void keepsWordWhenChunkStartsAtWordStart() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(1, 5, 11);

        List<String> textFragments = adapter.readText(datasetPath, chunk);

        assertEquals(List.of("spring"), textFragments);
    }

    @Test
    void doesNotReadNextWordWhenChunkEndsAtSeparator() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        Files.writeString(datasetPath, "java spring");

        FileSystemChunkTextReaderAdapter adapter = new FileSystemChunkTextReaderAdapter();

        FileChunk chunk = new FileChunk(0, 0, 5);

        List<String> textFragments = adapter.readText(datasetPath, chunk);

        assertEquals(List.of("java "), textFragments);
    }
}