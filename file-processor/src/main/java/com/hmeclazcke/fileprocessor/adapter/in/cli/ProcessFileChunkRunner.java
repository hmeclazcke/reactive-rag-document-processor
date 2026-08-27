package com.hmeclazcke.fileprocessor.adapter.in.cli;

import com.hmeclazcke.fileprocessor.application.ProcessFileChunkUseCase;
import com.hmeclazcke.fileprocessor.config.FileProcessorProperties;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCount;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.ProcessedFileChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.util.Comparator;
import java.util.Map;

public class ProcessFileChunkRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessFileChunkRunner.class);
    private static final int TOP_WORDS_TO_LOG = 20;
    private final ProcessFileChunkUseCase useCase;
    private final FileProcessorProperties properties;

    public ProcessFileChunkRunner(ProcessFileChunkUseCase useCase, FileProcessorProperties properties) {
        this.useCase = useCase;
        this.properties = properties;
    }

    private static String formatResult(String datasetId, String datasetPath, FileChunk chunk, ProcessedFileChunk result) {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        ChunkWordCount chunkWordCount = result.chunkWordCount();

        builder.append(lineSeparator)
                .append("Chunk processed").append(lineSeparator)
                .append("  datasetId: ").append(datasetId).append(lineSeparator)
                .append("  datasetPath: ").append(datasetPath).append(lineSeparator)
                .append("  chunkIndex: ").append(chunk.index()).append(lineSeparator)
                .append("  startByteInclusive: ").append(chunk.startByteInclusive()).append(lineSeparator)
                .append("  endByteExclusive: ").append(chunk.endByteExclusive()).append(lineSeparator)
                .append("  uniqueWords: ").append(chunkWordCount.wordCounts().size()).append(lineSeparator)
                .append("  ragChunks: ").append(result.ragChunkCount()).append(lineSeparator)
                .append(lineSeparator)
                .append("Top ").append(TOP_WORDS_TO_LOG).append(" words").append(lineSeparator);

        chunkWordCount.wordCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_WORDS_TO_LOG)
                .forEach(entry -> builder.append("  ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append(lineSeparator));

        return builder.toString();
    }

    @Override
    public void run(String... args) {
        FileChunk chunk = new FileChunk(
                properties.chunkIndex(),
                properties.startByteInclusive(),
                properties.endByteExclusive()
        );

        // .block(): Wait at the command-line entrypoint to turn the Mono into the job's final result.
        ProcessedFileChunk result = useCase.process(properties.datasetId(), properties.datasetPath(), chunk).block();

        LOGGER.info("{}", formatResult(properties.datasetId(), properties.datasetPath().toString(), chunk, result));
    }
}
