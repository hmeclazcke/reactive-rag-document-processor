package com.hmeclazcke.filegenerator.adapter.out.filesystem;

import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemDatasetFileAdapter implements DatasetFilePort {

    private static final String COULD_NOT_CREATE_DATASET_FILE = "Could not create dataset file";

    // The sample line must end with a line break so generated datasets do not cut words.
    private static final String SAMPLE_LINE = "java spring reactor mongo docker kubernetes\n";

    @Override
    public boolean exists(Path datasetPath) {
        return Files.exists(datasetPath);
    }

    @Override
    public void create(Path datasetPath, long minimumSizeBytes) {
        try {
            createDatasetFile(datasetPath, minimumSizeBytes);
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_CREATE_DATASET_FILE, exception);
        }
    }

    private void createDatasetFile(Path datasetPath, long minimumSizeBytes) throws IOException {
        createParentDirectories(datasetPath);

        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(datasetPath))) {
            writeLinesUntilMinimumSize(outputStream, minimumSizeBytes);
        }
    }

    private void createParentDirectories(Path datasetPath) throws IOException {
        Path parentDirectory = datasetPath.getParent();

        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
    }

    private void writeLinesUntilMinimumSize(OutputStream outputStream, long minimumSizeBytes) throws IOException {
        byte[] lineBytes = SAMPLE_LINE.getBytes(StandardCharsets.UTF_8);
        long writtenBytes = 0;

        while (writtenBytes < minimumSizeBytes) {
            outputStream.write(lineBytes);
            writtenBytes += lineBytes.length;
        }
    }
}