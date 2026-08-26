package com.hmeclazcke.filequeryapi;

import com.hmeclazcke.filequeryapi.adapter.out.mongodb.ChunkWordCountDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
class FileQueryApiIntegrationTest {

    @Container
    private static final MongoDBContainer mongo = new MongoDBContainer(
            DockerImageName.parse("mongo:8.3")
    ).withReplicaSet();

    private final ReactiveMongoTemplate mongoTemplate;
    private final HttpGraphQlTester graphQlTester;
    private static final String DATASET_ID = "dataset-test";

    @Autowired
    FileQueryApiIntegrationTest(ReactiveMongoTemplate mongoTemplate, HttpGraphQlTester graphQlTester) {
        this.mongoTemplate = mongoTemplate;
        this.graphQlTester = graphQlTester;
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.mongodb.uri",
                () -> mongo.getReplicaSetUrl("reactive_rag_test")
        );
    }

    @BeforeEach
    void setUp() {
        List<ChunkWordCountDocument> documents = List.of(
                new ChunkWordCountDocument("dataset-test:0:java", DATASET_ID, 0, "java", 3),
                new ChunkWordCountDocument("dataset-test:1:java", DATASET_ID, 1, "java", 2),
                new ChunkWordCountDocument("dataset-test:0:spring", DATASET_ID, 0, "spring", 4),
                new ChunkWordCountDocument("dataset-test:0:reactor", DATASET_ID, 0, "reactor", 1)
        );

        StepVerifier.create(
                        mongoTemplate.remove(new Query(), ChunkWordCountDocument.class)
                                .thenMany(mongoTemplate.insertAll(documents))
                )
                .expectNextCount(documents.size())
                .verifyComplete();
    }

    @Test
    void returnsTopWordsFromMongoThroughGraphQl() {
        graphQlTester.document("""
                        query {
                          topWords(limit: 2) {
                            word
                            count
                          }
                        }
                        """)
                .execute()
                .path("topWords[0].word").entity(String.class).isEqualTo("java")
                .path("topWords[0].count").entity(Integer.class).isEqualTo(5)
                .path("topWords[1].word").entity(String.class).isEqualTo("spring")
                .path("topWords[1].count").entity(Integer.class).isEqualTo(4);
    }
}