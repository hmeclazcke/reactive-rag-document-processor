package com.hmeclazcke.filequeryapi.config;

import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoDatasetQueryAdapter;
import com.hmeclazcke.filequeryapi.adapter.out.gemini.SpringAiGeminiDocumentAnswerAdapter;
import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoRagChunkQueryAdapter;
import com.hmeclazcke.filequeryapi.adapter.out.mongodb.MongoWordCountQueryAdapter;
import com.hmeclazcke.filequeryapi.adapter.out.qdrant.SpringAiRagChunkSearchAdapter;
import com.hmeclazcke.filequeryapi.application.AskDocumentUseCase;
import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.application.ListDatasetsUseCase;
import com.hmeclazcke.filequeryapi.application.SearchDocumentContextUseCase;
import com.hmeclazcke.filequeryapi.application.port.out.DocumentAnswerPort;
import com.hmeclazcke.filequeryapi.application.port.out.DatasetQueryPort;
import com.hmeclazcke.filequeryapi.application.port.out.RagChunkQueryPort;
import com.hmeclazcke.filequeryapi.application.port.out.RagChunkSearchPort;
import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
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

    @Bean
    public RagChunkQueryPort ragChunkQueryPort(ReactiveMongoTemplate mongoTemplate) {
        return new MongoRagChunkQueryAdapter(mongoTemplate);
    }

    @Bean
    public RagChunkSearchPort ragChunkSearchPort(VectorStore vectorStore) {
        return new SpringAiRagChunkSearchAdapter(vectorStore);
    }

    @Bean
    public DocumentAnswerPort documentAnswerPort(ChatModel chatModel) {
        return new SpringAiGeminiDocumentAnswerAdapter(chatModel);
    }

    @Bean
    public SearchDocumentContextUseCase searchDocumentContextUseCase(
            RagChunkSearchPort ragChunkSearch,
            RagChunkQueryPort ragChunkQuery,
            FileQueryApiProperties properties
    ) {
        return new SearchDocumentContextUseCase(
                ragChunkSearch,
                ragChunkQuery,
                properties.askDocument().retrievedChunkLimit()
        );
    }

    @Bean
    public AskDocumentUseCase askDocumentUseCase(
            SearchDocumentContextUseCase searchDocumentContext,
            DocumentAnswerPort documentAnswer
    ) {
        return new AskDocumentUseCase(searchDocumentContext, documentAnswer);
    }
}
