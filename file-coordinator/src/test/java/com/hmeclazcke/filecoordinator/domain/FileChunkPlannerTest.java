package com.hmeclazcke.filecoordinator.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hmeclazcke.filecoordinator.support.FileSizeTestUtils.megabytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileChunkPlannerTest {

    private final FileChunkPlanner planner = new FileChunkPlanner();

    @Test
    void createsChunksForExactDivision() {
        long fileSize = megabytes(10);
        long chunkSize = megabytes(5);

        List<FileChunk> chunks = planner.plan(fileSize, chunkSize);

        assertEquals(List.of(
                new FileChunk(0, 0, megabytes(5)),
                new FileChunk(1, megabytes(5), megabytes(10))
        ), chunks);
    }

    @Test
    void createsSmallerLastChunkWhenFileSizeDoesNotDivideExactly() {
        long fileSize = megabytes(11);
        long chunkSize = megabytes(5);

        List<FileChunk> chunks = planner.plan(fileSize, chunkSize);

        assertEquals(List.of(
                new FileChunk(0, 0, megabytes(5)),
                new FileChunk(1, megabytes(5), megabytes(10)),
                new FileChunk(2, megabytes(10), megabytes(11))
        ), chunks);
    }

    @Test
    void createsOneChunkWhenFileIsSmallerThanChunkSize() {
        long fileSize = megabytes(3);
        long chunkSize = megabytes(5);

        List<FileChunk> chunks = planner.plan(fileSize, chunkSize);

        assertEquals(List.of(
                new FileChunk(0, 0, megabytes(3))
        ), chunks);
    }

    @Test
    void rejectsZeroFileSize() {
        long fileSize = 0;
        long chunkSize = megabytes(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.plan(fileSize, chunkSize)
        );
    }

    @Test
    void rejectsZeroChunkSize() {
        long fileSize = megabytes(10);
        long chunkSize = 0;

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.plan(fileSize, chunkSize)
        );
    }
}