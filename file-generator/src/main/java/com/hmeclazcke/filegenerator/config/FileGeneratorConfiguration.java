package com.hmeclazcke.filegenerator.config;

import com.hmeclazcke.filegenerator.adapter.in.cli.GenerateDatasetRunner;
import com.hmeclazcke.filegenerator.adapter.out.filesystem.FileSystemDatasetFileAdapter;
import com.hmeclazcke.filegenerator.adapter.out.gemini.SpringAiGeminiGeneratedSeedTextAdapter;
import com.hmeclazcke.filegenerator.adapter.out.llm.LlmTextSeedProvider;
import com.hmeclazcke.filegenerator.adapter.out.local.LocalTextSeedProvider;
import com.hmeclazcke.filegenerator.application.DatasetGenerator;
import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;
import com.hmeclazcke.filegenerator.application.port.out.GeneratedSeedTextPort;
import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileGeneratorProperties.class)
public class FileGeneratorConfiguration {

    @Bean
    public DatasetFilePort datasetFilePort(TextSeedProviderPort textSeedProvider) {
        return new FileSystemDatasetFileAdapter(textSeedProvider);
    }

    @Bean
    public DatasetGenerator datasetGenerator(DatasetFilePort datasetFilePort) {
        return new DatasetGenerator(datasetFilePort);
    }

    @Bean
    public GenerateDatasetRunner generateDatasetRunner(
            DatasetGenerator datasetGenerator,
            FileGeneratorProperties properties
    ) {
        return new GenerateDatasetRunner(datasetGenerator, properties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "file-generator",
            name = "seed-provider",
            havingValue = "local",
            matchIfMissing = true
    )
    public TextSeedProviderPort localTextSeedProvider(FileGeneratorProperties properties) {
        return new LocalTextSeedProvider(properties.seedResourcePath());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "file-generator",
            name = "seed-provider",
            havingValue = "llm"
    )
    public TextSeedProviderPort llmTextSeedProvider(GeneratedSeedTextPort generatedSeedTextPort) {
        return new LlmTextSeedProvider(generatedSeedTextPort);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "file-generator",
            name = "seed-provider",
            havingValue = "llm"
    )
    public GeneratedSeedTextPort generatedSeedTextPort(ChatModel chatModel) {
        return new SpringAiGeminiGeneratedSeedTextAdapter(chatModel);
    }

}
