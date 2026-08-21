package com.hmeclazcke.filegenerator.adapter.in.cli;

import com.hmeclazcke.filegenerator.application.DatasetGenerator;
import com.hmeclazcke.filegenerator.config.FileGeneratorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

public class GenerateDatasetRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateDatasetRunner.class);

    private final DatasetGenerator datasetGenerator;
    private final FileGeneratorProperties properties;

    public GenerateDatasetRunner(DatasetGenerator datasetGenerator, FileGeneratorProperties properties) {
        this.datasetGenerator = datasetGenerator;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        LOGGER.info(
                "Generating dataset at {} with minimum size {} bytes",
                properties.datasetPath(),
                properties.minimumSizeBytes()
        );

        datasetGenerator.generate(properties.datasetPath(), properties.minimumSizeBytes());

        LOGGER.info("Dataset generation finished");
    }
}