package com.hmeclazcke.filecoordinator.adapter.in.cli;

import com.hmeclazcke.filecoordinator.application.PlanFileProcessingUseCase;
import com.hmeclazcke.filecoordinator.config.FileCoordinatorProperties;
import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

public class PlanFileProcessingRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanFileProcessingRunner.class);

    private final PlanFileProcessingUseCase useCase;
    private final FileCoordinatorProperties properties;

    public PlanFileProcessingRunner(PlanFileProcessingUseCase useCase, FileCoordinatorProperties properties) {
        this.useCase = useCase;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        ProcessingPlan plan = useCase.plan(properties.datasetPath(), properties.chunkSizeBytes());

        LOGGER.info("""
        
        Processing plan
          datasetPath: {}
          fileSizeBytes: {}
          chunkSizeBytes: {}
          workers: {}
        """,
                plan.datasetPath(),
                plan.fileSizeBytes(),
                plan.chunkSizeBytes(),
                plan.chunks().size()
        );

        for (FileChunk chunk : plan.chunks()) {
            long chunkSizeBytes = chunk.endByteExclusive() - chunk.startByteInclusive();

            LOGGER.info("""
            
            Chunk {}
              range: [{}, {})
              sizeBytes: {}
            """,
                    chunk.index(),
                    chunk.startByteInclusive(),
                    chunk.endByteExclusive(),
                    chunkSizeBytes
            );
        }
    }
}