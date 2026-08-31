package com.hmeclazcke.filequeryapi.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagChunkSourceTest {

    @Test
    void returnsFullTextWhenTextFitsPreviewLimit() {
        RagChunkSource source = sourceWithText("short source text");

        assertEquals("short source text", source.textPreview());
    }

    @Test
    void truncatesTextPreviewWhenTextExceedsPreviewLimit() {
        String text = "a".repeat(501);

        RagChunkSource source = sourceWithText(text);

        assertEquals(500, source.textPreview().length());
        assertEquals("a".repeat(497) + "...", source.textPreview());
    }

    @Test
    void failsWhenTextIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> sourceWithText(null)
        );

        assertEquals("text must not be null", exception.getMessage());
    }

    private RagChunkSource sourceWithText(String text) {
        return new RagChunkSource(
                1,
                "dataset:rag:0:1",
                0,
                1,
                100,
                200,
                text
        );
    }
}
