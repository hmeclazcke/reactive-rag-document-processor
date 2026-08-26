package com.hmeclazcke.filecoordinator.application;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;
import com.hmeclazcke.filecoordinator.application.port.out.DatasetMetadataRepositoryPort;
import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.FileChunkPlanner;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static com.hmeclazcke.filecoordinator.support.FileSizeTestUtils.megabytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlanFileProcessingUseCaseTest {

    private static final String DATASET_ID = "dataset-6g";
    private final FileChunkPlanner chunkPlanner = new FileChunkPlanner();

    @Test
    void createsProcessingPlanForDatasetFile() {
        Path datasetPath = Path.of("dataset.txt");
        long fileSize = megabytes(10);
        long chunkSize = megabytes(5);
        DatasetFileInspectorPort fileInspector = mock(DatasetFileInspectorPort.class);
        DatasetMetadataRepositoryPort metadataRepository = mock(DatasetMetadataRepositoryPort.class);
        PlanFileProcessingUseCase useCase = new PlanFileProcessingUseCase(fileInspector, chunkPlanner, metadataRepository);

        ProcessingPlan expectedPlan = new ProcessingPlan(
                DATASET_ID,
                datasetPath,
                fileSize,
                chunkSize,
                List.of(
                        new FileChunk(0, 0, megabytes(5)),
                        new FileChunk(1, megabytes(5), megabytes(10))
                )
        );

        when(fileInspector.size(datasetPath)).thenReturn(fileSize);

        when(metadataRepository.save(expectedPlan)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.plan(DATASET_ID, datasetPath, chunkSize))
                .expectNext(expectedPlan)
                .verifyComplete();

        verify(metadataRepository).save(expectedPlan);
    }
}