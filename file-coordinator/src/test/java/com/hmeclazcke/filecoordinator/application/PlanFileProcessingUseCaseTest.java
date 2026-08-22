package com.hmeclazcke.filecoordinator.application;

import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;
import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.FileChunkPlanner;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static com.hmeclazcke.filecoordinator.support.FileSizeTestUtils.megabytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanFileProcessingUseCaseTest {

    private final FileChunkPlanner chunkPlanner = new FileChunkPlanner();

    @Test
    void createsProcessingPlanForDatasetFile() {
        Path datasetPath = Path.of("dataset.txt");
        long fileSize = megabytes(10);
        long chunkSize = megabytes(5);
        DatasetFileInspectorPort fileInspector = mock(DatasetFileInspectorPort.class);
        PlanFileProcessingUseCase useCase = new PlanFileProcessingUseCase(fileInspector, chunkPlanner);

        when(fileInspector.size(datasetPath)).thenReturn(fileSize);

        ProcessingPlan plan = useCase.plan(datasetPath, chunkSize);

        assertEquals(new ProcessingPlan(
                datasetPath,
                fileSize,
                chunkSize,
                List.of(
                        new FileChunk(0, 0, megabytes(5)),
                        new FileChunk(1, megabytes(5), megabytes(10))
                )
        ), plan);
    }
}