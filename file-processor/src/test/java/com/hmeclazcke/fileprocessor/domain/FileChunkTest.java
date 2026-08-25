package com.hmeclazcke.fileprocessor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileChunkTest {

    @Test
    void failsWhenIndexIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunk(-1, 0, 100));
    }

    @Test
    void failsWhenStartByteInclusiveIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunk(0, -1, 100));
    }

    @Test
    void failsWhenEndByteExclusiveIsEqualToStartByteInclusive() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunk(0, 100, 100));
    }

    @Test
    void failsWhenEndByteExclusiveIsBeforeStartByteInclusive() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunk(0, 100, 99));
    }
}