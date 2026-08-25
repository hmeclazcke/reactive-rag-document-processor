package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkTextReaderSettingsTest {

    @Test
    void failsWhenMaxWordLengthBytesIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkTextReaderSettings(0, 1024));
    }

    @Test
    void failsWhenMaxWordLengthBytesIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkTextReaderSettings(-1, 1024));
    }

    @Test
    void failsWhenBufferSizeBytesIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkTextReaderSettings(1024, 0));
    }

    @Test
    void failsWhenBufferSizeBytesIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkTextReaderSettings(1024, -1));
    }
}