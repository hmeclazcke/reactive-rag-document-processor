package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.GetTopWordsUseCase;
import com.hmeclazcke.filequeryapi.config.GraphQlScalarConfiguration;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.when;

@GraphQlTest(WordCountGraphQlController.class)
@Import(GraphQlScalarConfiguration.class)
class WordCountGraphQlControllerTest {

    private static final String DATASET_ID = "dataset-test";
    private static final long LARGE_WORD_COUNT = 3_000_000_000L;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private GetTopWordsUseCase useCase;

    @Test
    void returnsTopWordsWithLongCounts() {
        when(useCase.getTopWords(DATASET_ID, 2)).thenReturn(Flux.just(
                new WordCount("java", LARGE_WORD_COUNT),
                new WordCount("reactor", 3)
        ));

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
                .path("topWords[0].count").entity(Long.class).isEqualTo(LARGE_WORD_COUNT)
                .path("topWords[1].word").entity(String.class).isEqualTo("reactor")
                .path("topWords[1].count").entity(Integer.class).isEqualTo(3);
    }

    @Test
    void returnsValidationErrorWhenLimitIsTooLarge() {
        when(useCase.getTopWords(DATASET_ID, 1000)).thenReturn(Flux.error(
                new IllegalArgumentException("limit must be less than or equal to 100")
        ));

        graphQlTester.document("""
                        query {
                          topWords(datasetId: "dataset-test", limit: 1000) {
                            word
                            count
                          }
                        }
                        """)
                .execute()
                .errors()
                .expect(error ->
                        error.getMessage().equals("limit must be less than or equal to 100")
                );
    }
}