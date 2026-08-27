package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileChunkProcessorSettingsTest {

    @Test
    void failsWhenMaxLineLengthBytesIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(0, 1024, 2000, 100));
    }

    @Test
    void failsWhenMaxLineLengthBytesIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(-1, 1024, 2000, 100));
    }

    @Test
    void failsWhenBufferSizeBytesIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, 0, 2000, 100));
    }

    @Test
    void failsWhenBufferSizeBytesIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, -1, 2000, 100));
    }

    @Test
    void failsWhenRagChunkMaxTextLengthCharactersIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, 1024, 0, 100));
    }

    @Test
    void failsWhenRagChunkMaxTextLengthCharactersIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, 1024, -1, 100));
    }

    @Test
    void failsWhenRagChunkBatchSizeIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, 1024, 2000, 0));
    }

    @Test
    void failsWhenRagChunkBatchSizeIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FileChunkProcessorSettings(1024, 1024, 2000, -1));
    }
}
