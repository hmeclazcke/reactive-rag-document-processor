package com.hmeclazcke.filecoordinator.adapter.out.mongodb;

import com.hmeclazcke.filecoordinator.domain.FileChunk;
import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static com.hmeclazcke.filecoordinator.support.FileSizeTestUtils.gigabytes;
import static com.hmeclazcke.filecoordinator.support.FileSizeTestUtils.megabytes;
import static org.mockito.Mockito.*;

class MongoDatasetMetadataRepositoryAdapterTest {

    private static final String DATASET_ID = "dataset-6g";
    private static final long FILE_SIZE_BYTES = gigabytes(6);
    private static final long CHUNK_SIZE_BYTES = megabytes(2_050);
    private static final int CHUNK_COUNT = 2;

    private final ReactiveMongoTemplate mongoTemplate = mock(ReactiveMongoTemplate.class);
    private final MongoDatasetMetadataRepositoryAdapter adapter =
            new MongoDatasetMetadataRepositoryAdapter(mongoTemplate);

    @Test
    void savesDatasetMetadata() {
        ProcessingPlan plan = new ProcessingPlan(
                DATASET_ID,
                Path.of("dataset.txt"),
                FILE_SIZE_BYTES,
                CHUNK_SIZE_BYTES,
                List.of(
                        new FileChunk(0, 0, CHUNK_SIZE_BYTES),
                        new FileChunk(1, CHUNK_SIZE_BYTES, FILE_SIZE_BYTES)
                )
        );
        DatasetMetadataDocument document = new DatasetMetadataDocument(
                DATASET_ID,
                "dataset.txt",
                FILE_SIZE_BYTES,
                CHUNK_SIZE_BYTES,
                CHUNK_COUNT
        );

        when(mongoTemplate.save(document)).thenReturn(Mono.just(document));

        StepVerifier.create(adapter.save(plan))
                .verifyComplete();

        verify(mongoTemplate).save(document);
    }
}