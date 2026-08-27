package com.hmeclazcke.fileprocessor.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamingWordCounterTest {

    private final StreamingWordCounter wordCounter = new StreamingWordCounter();

    @Test
    void countsWordsAcrossAcceptedCharacters() {
        "hello world hello reactor".codePoints().forEach(wordCounter::acceptCodePoint);

        assertEquals(Map.of(
                "hello", 2L,
                "world", 1L,
                "reactor", 1L
        ), wordCounter.result());
    }

    @Test
    void countsNormalizedWords() {
        "Java, java! JAVA".codePoints().forEach(wordCounter::acceptCodePoint);

        assertEquals(Map.of("java", 3L), wordCounter.result());
    }

    @Test
    void keepsUnicodeLettersInsideWords() {
        "\u00c1baco ni\u00f1o".codePoints().forEach(wordCounter::acceptCodePoint);

        assertEquals(Map.of(
                "\u00e1baco", 1L,
                "ni\u00f1o", 1L
        ), wordCounter.result());
    }

    @Test
    void normalizesUnicodeBeforeTokenizing() {
        "\u0130".codePoints().forEach(wordCounter::acceptCodePoint);

        assertEquals(Map.of("i", 1L), wordCounter.result());
    }
}
