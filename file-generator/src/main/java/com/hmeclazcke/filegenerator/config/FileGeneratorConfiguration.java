package com.hmeclazcke.filegenerator.config;

import com.hmeclazcke.filegenerator.adapter.in.cli.GenerateDatasetRunner;
import com.hmeclazcke.filegenerator.adapter.out.filesystem.FileSystemDatasetFileAdapter;
import com.hmeclazcke.filegenerator.application.DatasetGenerator;
import com.hmeclazcke.filegenerator.application.port.out.DatasetFilePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileGeneratorProperties.class)
public class FileGeneratorConfiguration {

    @Bean
    public DatasetFilePort datasetFilePort() {
        return new FileSystemDatasetFileAdapter();
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
}