package com.hmeclazcke.filequeryapi.config;

import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoDatasetQueryAdapter;
import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoWordCountQueryAdapter;
import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.application.ListDatasetsUseCase;
import com.hmeclazcke.filequeryapi.application.port.out.DatasetQueryPort;
import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
@EnableConfigurationProperties(FileQueryApiProperties.class)
public class FileQueryApiConfiguration {

    @Bean
    public WordCountQueryPort wordCountQueryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoWordCountQueryAdapter(mongoTemplate);
    }

    @Bean
    public GetTopWordsUseCase getTopWordsUseCase(
            WordCountQueryPort wordCountQuery,
            FileQueryApiProperties properties
    ) {
        return new GetTopWordsUseCase(
                wordCountQuery,
                properties.topWords().maxLimit()
        );
    }

    @Bean
    public DatasetQueryPort datasetQueryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoDatasetQueryAdapter(mongoTemplate);
    }

    @Bean
    public ListDatasetsUseCase listDatasetsUseCase(DatasetQueryPort datasetQuery) {
        return new ListDatasetsUseCase(datasetQuery);
    }
}