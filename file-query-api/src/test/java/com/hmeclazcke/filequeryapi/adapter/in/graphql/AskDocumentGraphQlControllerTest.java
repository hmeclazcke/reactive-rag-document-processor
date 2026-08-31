package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.AskDocumentUseCase;
import com.hmeclazcke.filequeryapi.application.DocumentAnswerGenerationException;
import com.hmeclazcke.filequeryapi.application.SearchDocumentContextUseCase;
import com.hmeclazcke.filequeryapi.config.GraphQlScalarConfiguration;
import com.hmeclazcke.filequeryapi.domain.DocumentAnswer;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@GraphQlTest(AskDocumentGraphQlController.class)
@Import(GraphQlScalarConfiguration.class)
class AskDocumentGraphQlControllerTest {

    private static final String DATASET_ID = "dataset-1g-gemini";
    private static final String QUESTION = "How does the processor create RAG chunks?";

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AskDocumentUseCase useCase;

    @MockitoBean
    private SearchDocumentContextUseCase searchDocumentContext;

    @Test
    void returnsDocumentAnswer() {
        when(useCase.ask(DATASET_ID, QUESTION)).thenReturn(Mono.just(
                new DocumentAnswer(
                        "The processor creates RAG chunks from complete lines.",
                        List.of(new RagChunkSource(
                                1,
                                "dataset-1g-gemini:rag:0:7",
                                0,
                                7,
                                700,
                                900,
                                "The processor creates RAG chunks from complete source lines."
                        ))
                )
        ));

        graphQlTester.document("""
                        query {
                          askDocument(
                            datasetId: "dataset-1g-gemini"
                            question: "How does the processor create RAG chunks?"
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
                .isEqualTo("The processor creates RAG chunks from complete lines.")
                .path("askDocument.sources[0].rank")
                .entity(Integer.class)
                .isEqualTo(1)
                .path("askDocument.sources[0].ragChunkId")
                .entity(String.class)
                .isEqualTo("dataset-1g-gemini:rag:0:7")
                .path("askDocument.sources[0].sourceChunkIndex")
                .entity(Integer.class)
                .isEqualTo(0)
                .path("askDocument.sources[0].ragChunkIndex")
                .entity(Integer.class)
                .isEqualTo(7)
                .path("askDocument.sources[0].startByteInclusive")
                .entity(Long.class)
                .isEqualTo(700L)
                .path("askDocument.sources[0].endByteExclusive")
                .entity(Long.class)
                .isEqualTo(900L)
                .path("askDocument.sources[0].textPreview")
                .entity(String.class)
                .isEqualTo("The processor creates RAG chunks from complete source lines.")
                .path("askDocument.sources[0].text")
                .entity(String.class)
                .isEqualTo("The processor creates RAG chunks from complete source lines.");
    }

    @Test
    void returnsDocumentContextWithoutGeneratingAnAnswer() {
        when(searchDocumentContext.search(DATASET_ID, QUESTION)).thenReturn(Mono.just(
                List.of(new RagChunkSource(
                        1,
                        "dataset-1g-gemini:rag:0:7",
                        0,
                        7,
                        700,
                        900,
                        "The processor creates RAG chunks from complete source lines."
                ))
        ));

        graphQlTester.document("""
                        query {
                          searchDocumentContext(
                            datasetId: "dataset-1g-gemini"
                            question: "How does the processor create RAG chunks?"
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
                .isEqualTo("dataset-1g-gemini:rag:0:7")
                .path("searchDocumentContext[0].textPreview")
                .entity(String.class)
                .isEqualTo("The processor creates RAG chunks from complete source lines.");
    }

    @Test
    void returnsValidationError() {
        when(useCase.ask(" ", QUESTION)).thenReturn(Mono.error(
                new IllegalArgumentException("datasetId must not be blank")
        ));

        graphQlTester.document("""
                        query {
                          askDocument(
                            datasetId: " "
                            question: "How does the processor create RAG chunks?"
                          ) {
                            answer
                          }
                        }
                        """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().equals("datasetId must not be blank"));
    }

    @Test
    void returnsGenerationErrorMessage() {
        when(useCase.ask(DATASET_ID, QUESTION)).thenReturn(Mono.error(
                new DocumentAnswerGenerationException(
                        "Gemini quota exceeded while generating the answer.",
                        new RuntimeException("429 quota exceeded")
                )
        ));

        graphQlTester.document("""
                        query {
                          askDocument(
                            datasetId: "dataset-1g-gemini"
                            question: "How does the processor create RAG chunks?"
                          ) {
                            answer
                          }
                        }
                        """)
                .execute()
                .errors()
                .expect(error -> error.getMessage().equals("Gemini quota exceeded while generating the answer."));
    }
}
