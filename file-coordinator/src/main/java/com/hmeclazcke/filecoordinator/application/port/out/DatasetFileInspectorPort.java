package com.hmeclazcke.filecoordinator.application.port.out;

import java.nio.file.Path;

public interface DatasetFileInspectorPort {

    long size(Path datasetPath);
}