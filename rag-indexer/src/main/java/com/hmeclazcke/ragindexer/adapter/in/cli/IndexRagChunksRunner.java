package com.hmeclazcke.ragindexer.adapter.in.cli;

import com.hmeclazcke.ragindexer.application.IndexRagChunksUseCase;
import com.hmeclazcke.ragindexer.config.RagIndexerProperties;
import com.hmeclazcke.ragindexer.domain.IndexRagChunksResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

public class IndexRagChunksRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexRagChunksRunner.class);

    private final IndexRagChunksUseCase useCase;
    private final RagIndexerProperties properties;

    public IndexRagChunksRunner(IndexRagChunksUseCase useCase, RagIndexerProperties properties) {
        this.useCase = useCase;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        LOGGER.info(
                "Indexing RAG chunks for dataset {} with batch size {}",
                properties.datasetId(),
                properties.batchSize()
        );

        // .block(): Wait at the command-line entrypoint to turn the Mono into the job's final result.
        IndexRagChunksResult result = useCase.index(properties.datasetId(), properties.batchSize()).block();

        LOGGER.info("""

        RAG chunks indexed
          datasetId: {}
          indexedChunks: {}
        """,
                properties.datasetId(),
                result.indexedChunks()
        );
    }
}
