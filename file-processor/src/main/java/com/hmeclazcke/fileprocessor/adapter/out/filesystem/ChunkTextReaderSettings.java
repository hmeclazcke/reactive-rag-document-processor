package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

public record ChunkTextReaderSettings(
        int maxWordLengthBytes,
        int bufferSizeBytes
) {
}