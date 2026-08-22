package com.hmeclazcke.filecoordinator.domain;

import java.util.ArrayList;
import java.util.List;

public class FileChunkPlanner {

    private static final String FILE_SIZE_MUST_BE_POSITIVE = "fileSizeBytes must be greater than zero";
    private static final String CHUNK_SIZE_MUST_BE_POSITIVE = "chunkSizeBytes must be greater than zero";

    public List<FileChunk> plan(long fileSizeBytes, long chunkSizeBytes) {
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException(FILE_SIZE_MUST_BE_POSITIVE);
        }

        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException(CHUNK_SIZE_MUST_BE_POSITIVE);
        }

        List<FileChunk> chunks = new ArrayList<>();

        // Chunks use [start, end) byte ranges, and the last chunk may be smaller than the configured size.
        for (long startByte = 0; startByte < fileSizeBytes; startByte += chunkSizeBytes) {
            long endByte = Math.min(startByte + chunkSizeBytes, fileSizeBytes);
            chunks.add(new FileChunk(chunks.size(), startByte, endByte));
        }

        return List.copyOf(chunks);
    }
}