package com.hmeclazcke.filecoordinator.adapter.out.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSystemDatasetFileInspectorAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsDatasetFileSizeInBytes() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");
        String content = "java spring reactor";
        FileSystemDatasetFileInspectorAdapter adapter = new FileSystemDatasetFileInspectorAdapter();

        Files.writeString(datasetPath, content);

        long size = adapter.size(datasetPath);

        assertEquals(Files.size(datasetPath), size);
    }
}