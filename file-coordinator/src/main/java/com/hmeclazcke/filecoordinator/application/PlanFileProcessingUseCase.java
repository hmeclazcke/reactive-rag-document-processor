package com.hmeclazcke.filecoordinator.application;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;
import com.hmeclazcke.filecoordinator.application.port.out.DatasetMetadataRepositoryPort;
import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.FileChunkPlanner;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.util.List;

public class PlanFileProcessingUseCase {

    private final DatasetFileInspectorPort fileInspector;
    private final FileChunkPlanner chunkPlanner;
    private final DatasetMetadataRepositoryPort metadataRepository;

    public PlanFileProcessingUseCase(DatasetFileInspectorPort fileInspector, FileChunkPlanner chunkPlanner, DatasetMetadataRepositoryPort metadataRepository) {
        this.fileInspector = fileInspector;
        this.chunkPlanner = chunkPlanner;
        this.metadataRepository = metadataRepository;
    }

    public Mono<ProcessingPlan> plan(String datasetId, Path datasetPath, long chunkSizeBytes) {
        return Mono.fromCallable(() -> fileInspector.size(datasetPath))
                // File size lookup is blocking filesystem I/O; boundedElastic keeps it off reactive worker threads.
                .subscribeOn(Schedulers.boundedElastic())
                .map(fileSizeBytes -> {
                    List<FileChunk> chunks = chunkPlanner.plan(fileSizeBytes, chunkSizeBytes);

                    return new ProcessingPlan(datasetId, datasetPath, fileSizeBytes, chunkSizeBytes, chunks);
                })
                .flatMap(plan -> metadataRepository.save(plan).thenReturn(plan));
    }
}