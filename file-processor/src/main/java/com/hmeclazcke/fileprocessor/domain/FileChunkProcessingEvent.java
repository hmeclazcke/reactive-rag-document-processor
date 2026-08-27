package com.hmeclazcke.fileprocessor.domain;

// Internal processing output emitted while scanning one file chunk.
public sealed interface FileChunkProcessingEvent permits RagChunkBatch, ChunkWordCountsComputed {
}
