package com.hmeclazcke.filegenerator.adapter.out.filesystem;

import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;
import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemDatasetFileAdapter implements DatasetFilePort {

    private static final String COULD_NOT_CREATE_DATASET_FILE = "Could not create dataset file";
    private static final String PARTIAL_FILE_SUFFIX = ".part";

    private final TextSeedProviderPort textSeedProvider;

    public FileSystemDatasetFileAdapter(TextSeedProviderPort textSeedProvider) {
        this.textSeedProvider = textSeedProvider;
    }

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
        Path partialDatasetPath = datasetPath.resolveSibling(datasetPath.getFileName() + PARTIAL_FILE_SUFFIX);

        try {
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(partialDatasetPath))) {
                writeLinesUntilMinimumSize(outputStream, minimumSizeBytes);
            }

            Files.move(partialDatasetPath, datasetPath);
        } catch (IOException | RuntimeException exception) {
            deletePartialFile(partialDatasetPath, exception);
            throw exception;
        }
    }

    private void deletePartialFile(Path partialDatasetPath, Exception originalException) {
        try {
            Files.deleteIfExists(partialDatasetPath);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private void createParentDirectories(Path datasetPath) throws IOException {
        Path parentDirectory = datasetPath.getParent();

        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
    }

    private void writeLinesUntilMinimumSize(OutputStream outputStream, long minimumSizeBytes) throws IOException {
        long writtenBytes = 0;

        while (writtenBytes < minimumSizeBytes) {
            byte[] lineBytes = textSeedProvider.nextLine().getBytes(StandardCharsets.UTF_8);

            outputStream.write(lineBytes);
            writtenBytes += lineBytes.length;
        }
    }
}
