package com.hmeclazcke.fileprocessor.config;

import com.hmeclazcke.fileprocessor.adapter.in.cli.ProcessFileChunkRunner;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.ChunkWordCounterSettings;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.FileSystemChunkWordCounterAdapter;
import com.hmeclazcke.fileprocessor.adapter.out.mongodb.MongoChunkWordCountRepositoryAdapter;
import com.hmeclazcke.fileprocessor.application.ProcessFileChunkUseCase;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCounterPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(FileProcessorProperties.class)
public class FileProcessorConfiguration {

    @Bean
    public ChunkWordCounterSettings chunkWordCounterSettings(FileProcessorProperties properties) {
        return new ChunkWordCounterSettings(
                properties.maxLineLengthBytes(),
                properties.bufferSizeBytes()
        );
    }

    @Bean
    public ChunkWordCounterPort chunkWordCounterPort(ChunkWordCounterSettings settings) {
        return new FileSystemChunkWordCounterAdapter(settings);
    }

    @Bean
    public ChunkWordCountRepositoryPort chunkWordCountRepositoryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoChunkWordCountRepositoryAdapter(mongoTemplate);
    }

    @Bean
    public ProcessFileChunkUseCase processFileChunkUseCase(
            ChunkWordCounterPort wordCounter,
            ChunkWordCountRepositoryPort repository
    ) {
        return new ProcessFileChunkUseCase(wordCounter, repository);
    }

    @Bean
    public ProcessFileChunkRunner processFileChunkRunner(
            ProcessFileChunkUseCase useCase,
            FileProcessorProperties properties
    ) {
        return new ProcessFileChunkRunner(useCase, properties);
    }
}
