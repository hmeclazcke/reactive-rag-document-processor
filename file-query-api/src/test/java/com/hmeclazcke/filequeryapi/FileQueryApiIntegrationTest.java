package com.hmeclazcke.filequeryapi;

import com.hmeclazcke.filequeryapi.adapter.out.mongodb.ChunkWordCountDocument;
import com.hmeclazcke.filequeryapi.adapter.out.mongodb.DatasetDocument;
import com.hmeclazcke.filequeryapi.adapter.out.mongodb.RagChunkDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    private static final String OTHER_DATASET_ID = "other-dataset";
    private static final String DATASET_PATH =
            "C:/projects/reactive-rag-document-processor/data/dataset-test.txt";
    private static final long DATASET_SIZE_BYTES = 6L * 1024 * 1024 * 1024;
    private static final long CHUNK_SIZE_BYTES = 2_150_000_000L;
    private static final int CHUNK_COUNT = 3;
    private static final String FIRST_RAG_CHUNK_ID = "dataset-test:rag:0:1";
    private static final String SECOND_RAG_CHUNK_ID = "dataset-test:rag:0:2";
    private static final String FIRST_RAG_CHUNK_TEXT =
            "The processor reads the source chunk once.";
    private static final String SECOND_RAG_CHUNK_TEXT =
            "RAG chunks are built from complete source lines.";

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private ChatModel chatModel;

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
        registry.add("spring.ai.model.chat", () -> "none");
        registry.add("spring.ai.model.embedding", () -> "none");
        registry.add("spring.ai.vectorstore.type", () -> "none");
    }

    @BeforeEach
    void setUp() {
        List<ChunkWordCountDocument> wordCountDocuments = List.of(
                new ChunkWordCountDocument("dataset-test:0:java", DATASET_ID, 0, "java", 3),
                new ChunkWordCountDocument("dataset-test:1:java", DATASET_ID, 1, "java", 2),
                new ChunkWordCountDocument("dataset-test:0:spring", DATASET_ID, 0, "spring", 4),
                new ChunkWordCountDocument("dataset-test:0:reactor", DATASET_ID, 0, "reactor", 1),
                new ChunkWordCountDocument("other-dataset:0:python", OTHER_DATASET_ID, 0, "python", 999)
        );

        DatasetDocument datasetDocument = new DatasetDocument(
                DATASET_ID,
                DATASET_PATH,
                DATASET_SIZE_BYTES,
                CHUNK_SIZE_BYTES,
                CHUNK_COUNT
        );

        List<RagChunkDocument> ragChunkDocuments = List.of(
                new RagChunkDocument(
                        FIRST_RAG_CHUNK_ID,
                        DATASET_ID,
                        0,
                        1,
                        FIRST_RAG_CHUNK_TEXT,
                        100,
                        140
                ),
                new RagChunkDocument(
                        SECOND_RAG_CHUNK_ID,
                        DATASET_ID,
                        0,
                        2,
                        SECOND_RAG_CHUNK_TEXT,
                        141,
                        190
                )
        );

        StepVerifier.create(
                        mongoTemplate.remove(new Query(), ChunkWordCountDocument.class)
                                .then(mongoTemplate.remove(new Query(), DatasetDocument.class))
                                .then(mongoTemplate.remove(new Query(), RagChunkDocument.class))
                                .thenMany(mongoTemplate.insertAll(wordCountDocuments))
                                .thenMany(mongoTemplate.insertAll(ragChunkDocuments))
                                .then(mongoTemplate.insert(datasetDocument))
                )
                .expectNext(datasetDocument)
                .verifyComplete();
    }

    @Test
    void returnsTopWordsForRequestedDatasetFromMongoThroughGraphQl() {
        graphQlTester.document("""
                        query {
                          topWords(datasetId: "dataset-test", limit: 2) {
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

    @Test
    void returnsDatasetsFromMongoThroughGraphQl() {
        graphQlTester.document("""
                        query {
                          datasets {
                            datasetId
                            path
                            fileSizeBytes
                            chunkSizeBytes
                            chunkCount
                          }
                        }
                        """)
                .execute()
                .path("datasets[0].datasetId").entity(String.class).isEqualTo(DATASET_ID)
                .path("datasets[0].path").entity(String.class).isEqualTo(DATASET_PATH)
                .path("datasets[0].fileSizeBytes").entity(Long.class).isEqualTo(DATASET_SIZE_BYTES)
                .path("datasets[0].chunkSizeBytes").entity(Long.class).isEqualTo(CHUNK_SIZE_BYTES)
                .path("datasets[0].chunkCount").entity(Integer.class).isEqualTo(CHUNK_COUNT);
    }

    @Test
    void returnsDocumentAnswerFromRetrievedRagChunksThroughGraphQl() {
        String answer = "The processor creates RAG chunks from complete source lines.";

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document(
                        "document-2",
                        SECOND_RAG_CHUNK_TEXT,
                        Map.of("ragChunkId", SECOND_RAG_CHUNK_ID)
                ),
                new Document(
                        "document-1",
                        FIRST_RAG_CHUNK_TEXT,
                        Map.of("ragChunkId", FIRST_RAG_CHUNK_ID)
                )
        ));
        when(chatModel.call(anyString())).thenReturn(answer);

        graphQlTester.document("""
                        query {
                          askDocument(
                            datasetId: "dataset-test"
                            question: "How are RAG chunks created?"
                          ) {
                            answer
                            sources {
                              rank
                              ragChunkId
                              sourceChunkIndex
                              ragChunkIndex
                              startByteInclusive
                              endByteExclusive
                              textPreview
                              text
                            }
                          }
                        }
                        """)
                .execute()
                .path("askDocument.answer")
                .entity(String.class)
                .isEqualTo(answer)
                .path("askDocument.sources[0].rank")
                .entity(Integer.class)
                .isEqualTo(1)
                .path("askDocument.sources[0].ragChunkId")
                .entity(String.class)
                .isEqualTo(SECOND_RAG_CHUNK_ID)
                .path("askDocument.sources[0].textPreview")
                .entity(String.class)
                .isEqualTo(SECOND_RAG_CHUNK_TEXT)
                .path("askDocument.sources[0].text")
                .entity(String.class)
                .isEqualTo(SECOND_RAG_CHUNK_TEXT)
                .path("askDocument.sources[1].rank")
                .entity(Integer.class)
                .isEqualTo(2)
                .path("askDocument.sources[1].ragChunkId")
                .entity(String.class)
                .isEqualTo(FIRST_RAG_CHUNK_ID)
                .path("askDocument.sources[1].text")
                .entity(String.class)
                .isEqualTo(FIRST_RAG_CHUNK_TEXT);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("How are RAG chunks created?"));
        assertTrue(prompt.indexOf(SECOND_RAG_CHUNK_TEXT) < prompt.indexOf(FIRST_RAG_CHUNK_TEXT));
    }

    @Test
    void returnsRetrievedContextWithoutCallingChatModelThroughGraphQl() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document(
                        "document-2",
                        SECOND_RAG_CHUNK_TEXT,
                        Map.of("ragChunkId", SECOND_RAG_CHUNK_ID)
                )
        ));

        graphQlTester.document("""
                        query {
                          searchDocumentContext(
                            datasetId: "dataset-test"
                            question: "How are RAG chunks created?"
                          ) {
                            rank
                            ragChunkId
                            sourceChunkIndex
                            ragChunkIndex
                            startByteInclusive
                            endByteExclusive
                            textPreview
                          }
                        }
                        """)
                .execute()
                .path("searchDocumentContext[0].rank")
                .entity(Integer.class)
                .isEqualTo(1)
                .path("searchDocumentContext[0].ragChunkId")
                .entity(String.class)
                .isEqualTo(SECOND_RAG_CHUNK_ID)
                .path("searchDocumentContext[0].textPreview")
                .entity(String.class)
                .isEqualTo(SECOND_RAG_CHUNK_TEXT);

        verifyNoInteractions(chatModel);
    }
}
