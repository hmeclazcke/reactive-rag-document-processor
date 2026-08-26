package com.hmeclazcke.filecoordinator.config;

import com.hmeclazcke.filecoordinator.adapter.in.cli.PlanFileProcessingRunner;
import com.hmeclazcke.filecoordinator.adapter.out.filesystem.FileSystemDatasetFileInspectorAdapter;
import com.hmeclazcke.filecoordinator.adapter.out.mongodb.MongoDatasetMetadataRepositoryAdapter;
import com.hmeclazcke.filecoordinator.application.PlanFileProcessingUseCase;
import com.hmeclazcke.filecoordinator.application.port.out.DatasetFileInspectorPort;
import com.hmeclazcke.filecoordinator.application.port.out.DatasetMetadataRepositoryPort;
import com.hmeclazcke.filecoordinator.domain.FileChunkPlanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(FileCoordinatorProperties.class)
public class FileCoordinatorConfiguration {

    @Bean
    public DatasetFileInspectorPort datasetFileInspectorPort() {
        return new FileSystemDatasetFileInspectorAdapter();
    }

    @Bean
    public FileChunkPlanner fileChunkPlanner() {
        return new FileChunkPlanner();
    }

    @Bean
    public PlanFileProcessingUseCase planFileProcessingUseCase(
            DatasetFileInspectorPort fileInspector,
            FileChunkPlanner chunkPlanner,
            DatasetMetadataRepositoryPort metadataRepository
    ) {
        return new PlanFileProcessingUseCase(fileInspector, chunkPlanner, metadataRepository);
    }

    @Bean
    public PlanFileProcessingRunner planFileProcessingRunner(
            PlanFileProcessingUseCase useCase,
            FileCoordinatorProperties properties
    ) {
        return new PlanFileProcessingRunner(useCase, properties);
    }

    @Bean
    public DatasetMetadataRepositoryPort datasetMetadataRepositoryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoDatasetMetadataRepositoryAdapter(mongoTemplate);
    }
}