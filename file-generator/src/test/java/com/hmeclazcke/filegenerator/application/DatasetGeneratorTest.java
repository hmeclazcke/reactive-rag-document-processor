package com.hmeclazcke.filegenerator.application;

import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetGeneratorTest {

    @Test
    void createsDatasetWhenItDoesNotExist() {
        Path datasetPath = Path.of("dataset.txt");
        long minimumSizeBytes = 128;
        DatasetFilePort datasetFilePort = mock(DatasetFilePort.class);
        DatasetGenerator generator = new DatasetGenerator(datasetFilePort);

        when(datasetFilePort.exists(datasetPath)).thenReturn(false);

        generator.generate(datasetPath, minimumSizeBytes);

        verify(datasetFilePort).create(datasetPath, minimumSizeBytes);
    }

    @Test
    void doesNotCreateDatasetWhenItAlreadyExists() {
        Path datasetPath = Path.of("dataset.txt");
        long minimumSizeBytes = 128;
        DatasetFilePort datasetFilePort = mock(DatasetFilePort.class);
        DatasetGenerator generator = new DatasetGenerator(datasetFilePort);

        when(datasetFilePort.exists(datasetPath)).thenReturn(true);

        generator.generate(datasetPath, minimumSizeBytes);

        verify(datasetFilePort, never()).create(datasetPath, minimumSizeBytes);
    }
}