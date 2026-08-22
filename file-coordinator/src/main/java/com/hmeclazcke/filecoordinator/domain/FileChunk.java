package com.hmeclazcke.filecoordinator.domain;

public record FileChunk(
        int index,
        long startByteInclusive,
        long endByteExclusive
) {
}