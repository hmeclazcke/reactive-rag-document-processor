package com.hmeclazcke.filecoordinator.adapter.out.filesystem;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemDatasetFileInspectorAdapter implements DatasetFileInspectorPort {

    private static final String COULD_NOT_READ_DATASET_FILE_SIZE = "Could not read dataset file size";

    @Override
    public long size(Path datasetPath) {
        try {
            return Files.size(datasetPath);
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_READ_DATASET_FILE_SIZE, exception);
        }
    }
}