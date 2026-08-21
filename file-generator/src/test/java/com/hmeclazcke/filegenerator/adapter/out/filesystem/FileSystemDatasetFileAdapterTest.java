package com.hmeclazcke.filegenerator.adapter.out.filesystem;

import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSystemDatasetFileAdapterTest {

    private static final String SAMPLE_LINE = "java spring reactor\n";
    private static final long MINIMUM_SIZE_BYTES = 128;


    @TempDir
    Path tempDir;

    @Test
    void createsDatasetFileWithAtLeastMinimumSize() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");

        TextSeedProviderPort textSeedProvider = mock(TextSeedProviderPort.class);
        when(textSeedProvider.nextLine()).thenReturn(SAMPLE_LINE);
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter(textSeedProvider);

        adapter.create(datasetPath, MINIMUM_SIZE_BYTES);

        assertTrue(Files.exists(datasetPath));
        assertTrue(Files.size(datasetPath) >= MINIMUM_SIZE_BYTES);
    }

    @Test
    void returnsTrueWhenDatasetFileExists() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");

        TextSeedProviderPort textSeedProvider = mock(TextSeedProviderPort.class);
        when(textSeedProvider.nextLine()).thenReturn(SAMPLE_LINE);
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter(textSeedProvider);

        Files.createFile(datasetPath);

        assertTrue(adapter.exists(datasetPath));
    }

    @Test
    void returnsFalseWhenDatasetFileDoesNotExist() {
        Path datasetPath = tempDir.resolve("dataset.txt");

        TextSeedProviderPort textSeedProvider = mock(TextSeedProviderPort.class);
        when(textSeedProvider.nextLine()).thenReturn(SAMPLE_LINE);
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter(textSeedProvider);

        assertFalse(adapter.exists(datasetPath));
    }

    @Test
    void endsGeneratedDatasetWithLineBreak() throws Exception {
        Path datasetPath = tempDir.resolve("dataset.txt");

        TextSeedProviderPort textSeedProvider = mock(TextSeedProviderPort.class);
        when(textSeedProvider.nextLine()).thenReturn(SAMPLE_LINE);
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter(textSeedProvider);

        adapter.create(datasetPath, MINIMUM_SIZE_BYTES);

        String content = Files.readString(datasetPath);

        assertTrue(content.endsWith(System.lineSeparator()) || content.endsWith("\n"));
    }

    @Test
    void createsParentDirectoriesWhenTheyDoNotExist() throws Exception {
        Path datasetPath = tempDir.resolve("datasets/generated/dataset.txt");

        TextSeedProviderPort textSeedProvider = mock(TextSeedProviderPort.class);
        when(textSeedProvider.nextLine()).thenReturn(SAMPLE_LINE);
        FileSystemDatasetFileAdapter adapter = new FileSystemDatasetFileAdapter(textSeedProvider);

        adapter.create(datasetPath, MINIMUM_SIZE_BYTES);

        assertTrue(Files.exists(datasetPath));
    }
}