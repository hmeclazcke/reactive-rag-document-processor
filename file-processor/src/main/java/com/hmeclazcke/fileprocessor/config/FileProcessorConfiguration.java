package com.hmeclazcke.fileprocessor.config;

import com.hmeclazcke.fileprocessor.adapter.in.cli.ProcessFileChunkRunner;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.ChunkTextReaderSettings;
import com.hmeclazcke.fileprocessor.adapter.out.filesystem.FileSystemChunkTextReaderAdapter;
import com.hmeclazcke.fileprocessor.adapter.out.mongodb.MongoChunkWordCountRepositoryAdapter;
import com.hmeclazcke.fileprocessor.application.ProcessFileChunkUseCase;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCountRepositoryPort;
import com.hmeclazcke.fileprocessor.domain.WordCounter;
import com.hmeclazcke.fileprocessor.domain.WordTokenizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(FileProcessorProperties.class)
public class FileProcessorConfiguration {

    @Bean
    public ChunkTextReaderSettings chunkTextReaderSettings(FileProcessorProperties properties) {
        return new ChunkTextReaderSettings(
                properties.maxLineLengthBytes(),
                properties.bufferSizeBytes()
        );
    }

    @Bean
    public ChunkTextReaderPort chunkTextReaderPort(ChunkTextReaderSettings settings) {
        return new FileSystemChunkTextReaderAdapter(settings);
    }

    @Bean
    public WordTokenizer wordTokenizer() {
        return new WordTokenizer();
    }

    @Bean
    public WordCounter wordCounter(WordTokenizer tokenizer) {
        return new WordCounter(tokenizer);
    }

    @Bean
    public ChunkWordCountRepositoryPort chunkWordCountRepositoryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoChunkWordCountRepositoryAdapter(mongoTemplate);
    }

    @Bean
    public ProcessFileChunkUseCase processFileChunkUseCase(
            ChunkTextReaderPort textReader,
            WordCounter wordCounter,
            ChunkWordCountRepositoryPort repository
    ) {
        return new ProcessFileChunkUseCase(textReader, wordCounter, repository);
    }

    @Bean
    public ProcessFileChunkRunner processFileChunkRunner(
            ProcessFileChunkUseCase useCase,
            FileProcessorProperties properties
    ) {
        return new ProcessFileChunkRunner(useCase, properties);
    }
}