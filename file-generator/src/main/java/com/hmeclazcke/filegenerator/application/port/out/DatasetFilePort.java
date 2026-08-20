package com.hmeclazcke.filegenerator.application.port.out;

import java.nio.file.Path;

public interface DatasetFilePort {

    boolean exists(Path datasetPath);

    void create(Path datasetPath, long minimumSizeBytes);
}