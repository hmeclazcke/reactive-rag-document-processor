package com.hmeclazcke.ragindexer.config;

import com.hmeclazcke.ragindexer.adapter.in.cli.IndexRagChunksRunner;
import com.hmeclazcke.ragindexer.adapter.out.mongodb.MongoRagChunkQueryAdapter;
import com.hmeclazcke.ragindexer.adapter.out.qdrant.SpringAiRagChunkIndexAdapter;
import com.hmeclazcke.ragindexer.application.IndexRagChunksUseCase;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkIndexPort;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(RagIndexerProperties.class)
public class RagIndexerConfiguration {

    @Bean
    public RagChunkQueryPort ragChunkQueryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoRagChunkQueryAdapter(mongoTemplate);
    }

    @Bean
    public RagChunkIndexPort ragChunkIndexPort(VectorStore vectorStore) {
        return new SpringAiRagChunkIndexAdapter(vectorStore);
    }

    @Bean
    public IndexRagChunksUseCase indexRagChunksUseCase(
            RagChunkQueryPort ragChunkQueryPort,
            RagChunkIndexPort ragChunkIndexPort
    ) {
        return new IndexRagChunksUseCase(
                ragChunkQueryPort,
                ragChunkIndexPort
        );
    }

    @Bean
    public IndexRagChunksRunner indexRagChunksRunner(
            IndexRagChunksUseCase useCase,
            RagIndexerProperties properties
    ) {
        return new IndexRagChunksRunner(useCase, properties);
    }
}
