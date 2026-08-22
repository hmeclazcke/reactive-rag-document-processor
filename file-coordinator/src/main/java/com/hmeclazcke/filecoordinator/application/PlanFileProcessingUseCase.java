package com.hmeclazcke.filecoordinator.application;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;
import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.FileChunkPlanner;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;

import java.nio.file.Path;
import java.util.List;

public class PlanFileProcessingUseCase {

    private final DatasetFileInspectorPort fileInspector;
    private final FileChunkPlanner chunkPlanner;

    public PlanFileProcessingUseCase(DatasetFileInspectorPort fileInspector, FileChunkPlanner chunkPlanner) {
        this.fileInspector = fileInspector;
        this.chunkPlanner = chunkPlanner;
    }

    public ProcessingPlan plan(Path datasetPath, long chunkSizeBytes) {
        long fileSizeBytes = fileInspector.size(datasetPath);
        List<FileChunk> chunks = chunkPlanner.plan(fileSizeBytes, chunkSizeBytes);

        return new ProcessingPlan(datasetPath, fileSizeBytes, chunkSizeBytes, chunks);
    }
}