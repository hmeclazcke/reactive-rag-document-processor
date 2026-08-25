package com.hmeclazcke.filequeryapi.config;

import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoWordCountQueryAdapter;
import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
public class FileQueryApiConfiguration {

    @Bean
    public WordCountQueryPort wordCountQueryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoWordCountQueryAdapter(mongoTemplate);
    }

    @Bean
    public GetTopWordsUseCase getTopWordsUseCase(WordCountQueryPort wordCountQuery) {
        return new GetTopWordsUseCase(wordCountQuery);
    }
}