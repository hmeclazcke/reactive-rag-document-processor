package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.ListDatasetsUseCase;
import com.hmeclazcke.filequeryapi.config.GraphQlScalarConfiguration;
import com.hmeclazcke.filequeryapi.domain.Dataset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.when;

@GraphQlTest(DatasetGraphQlController.class)
@Import(GraphQlScalarConfiguration.class)
class DatasetGraphQlControllerTest {

    private static final String DATASET_ID = "dataset-6g";
    private static final String DATASET_PATH =
            "C:/projects/reactive-rag-document-processor/data/dataset-6g.txt";
    private static final long SIX_GIB_BYTES = 6L * 1024 * 1024 * 1024;
    private static final long THREE_CHUNK_SIZE_BYTES = 2_150_000_000L;
    private static final int CHUNK_COUNT = 3;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ListDatasetsUseCase useCase;

    @Test
    void returnsAvailableDatasets() {
        when(useCase.listDatasets()).thenReturn(Flux.just(
                new Dataset(
                        DATASET_ID,
                        DATASET_PATH,
                        SIX_GIB_BYTES,
                        THREE_CHUNK_SIZE_BYTES,
                        CHUNK_COUNT
                )
        ));

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
                .path("datasets[0].fileSizeBytes").entity(Long.class).isEqualTo(SIX_GIB_BYTES)
                .path("datasets[0].chunkSizeBytes").entity(Long.class).isEqualTo(THREE_CHUNK_SIZE_BYTES)
                .path("datasets[0].chunkCount").entity(Integer.class).isEqualTo(CHUNK_COUNT);
    }
}