package com.hmeclazcke.fileprocessor.domain;

public record FileChunk(
        int index,
        long startByteInclusive,
        long endByteExclusive
) {
}