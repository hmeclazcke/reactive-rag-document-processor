package com.hmeclazcke.filegenerator.application;

import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;

import java.nio.file.Path;

public class DatasetGenerator {

    private final DatasetFilePort datasetFilePort;

    public DatasetGenerator(DatasetFilePort datasetFilePort) {
        this.datasetFilePort = datasetFilePort;
    }

    public void generate(Path datasetPath, long minimumSizeBytes) {
        if (datasetFilePort.exists(datasetPath)) {
            return;
        }

        datasetFilePort.create(datasetPath, minimumSizeBytes);
    }
}