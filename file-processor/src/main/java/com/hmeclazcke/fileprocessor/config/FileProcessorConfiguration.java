package com.hmeclazcke.fileprocessor.config;

import com.hmeclazcke.fileprocessor.adapter.in.cli.ProcessFileChunkRunner;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.FileChunkProcessorSettings;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.FileSystemChunkProcessorAdapter;
import com.hmeclazcke.fileprocessor.adapter.out.mongodb.MongoChunkWordCountRepositoryAdapter;
import com.hmeclazcke.fileprocessor.adapter.out.mongodb.MongoRagChunkRepositoryAdapter;
import com.hmeclazcke.fileprocessor.application.ProcessFileChunkUseCase;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.application.port.out.FileChunkProcessorPort;
import com.hmeclazcke.fileprocessor.application.port.out.RagChunkRepositoryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(FileProcessorProperties.class)
public class FileProcessorConfiguration {

    @Bean
    public FileChunkProcessorSettings fileChunkProcessorSettings(FileProcessorProperties properties) {
        return new FileChunkProcessorSettings(
                properties.maxLineLengthBytes(),
                properties.bufferSizeBytes(),
                properties.ragChunkMaxTextLengthCharacters(),
                properties.ragChunkBatchSize()
        );
    }

    @Bean
    public FileChunkProcessorPort fileChunkProcessorPort(FileChunkProcessorSettings settings) {
        return new FileSystemChunkProcessorAdapter(settings);
    }

    @Bean
    public ChunkWordCountRepositoryPort chunkWordCountRepositoryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoChunkWordCountRepositoryAdapter(mongoTemplate);
    }

    @Bean
    public RagChunkRepositoryPort ragChunkRepositoryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoRagChunkRepositoryAdapter(mongoTemplate);
    }

    @Bean
    public ProcessFileChunkUseCase processFileChunkUseCase(
            FileChunkProcessorPort chunkProcessor,
            ChunkWordCountRepositoryPort wordCountRepository,
            RagChunkRepositoryPort ragChunkRepository
    ) {
        return new ProcessFileChunkUseCase(chunkProcessor, wordCountRepository, ragChunkRepository);
    }

    @Bean
    public ProcessFileChunkRunner processFileChunkRunner(
            ProcessFileChunkUseCase useCase,
            FileProcessorProperties properties
    ) {
        return new ProcessFileChunkRunner(useCase, properties);
    }
}
