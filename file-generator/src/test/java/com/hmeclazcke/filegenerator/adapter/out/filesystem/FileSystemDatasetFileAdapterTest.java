package com.hmeclazcke.filegenerator.adapter.out.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemDatasetFileAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void createsDatasetFileWithAtLeastMinimumSize() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        long minimumSizeBytes = 128;
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter();

        adapter.create(datasetPath, minimumSizeBytes);

        assertTrue(Files.exists(datasetPath));
        assertTrue(Files.size(datasetPath) >= minimumSizeBytes);
    }

    @Test
    void returnsTrueWhenDatasetFileExists() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter();

        Files.createFile(datasetPath);

        assertTrue(adapter.exists(datasetPath));
    }

    @Test
    void returnsFalseWhenDatasetFileDoesNotExist() {
        Path datasetPath = tempDir.resolve("dataset.txt");
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter();

        assertFalse(adapter.exists(datasetPath));
    }

    @Test
    void endsGeneratedDatasetWithLineBreak() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        long minimumSizeBytes = 128;
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter();

        adapter.create(datasetPath, minimumSizeBytes);

        String content = Files.readString(datasetPath);

        assertTrue(content.endsWith(System.lineSeparator()) || content.endsWith("\n"));
    }

    @Test
    void createsParentDirectoriesWhenTheyDoNotExist() throws Exception {
        Path datasetPath = tempDir.resolve("datasets/generated/dataset.txt");
        long minimumSizeBytes = 128;
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter();

        adapter.create(datasetPath, minimumSizeBytes);

        assertTrue(Files.exists(datasetPath));
    }
}