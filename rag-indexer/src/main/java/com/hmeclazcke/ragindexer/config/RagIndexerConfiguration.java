package com.hmeclazcke.ragindexer.config;

import com.hmeclazcke.ragindexer.adapter.out.mongodb.MongoRagChunkQueryAdapter;
import com.hmeclazcke.ragindexer.application.port.out.RagChunkQueryPort;
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
}
