package com.hmeclazcke.fileprocessor.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordTokenizerTest {

    private final WordTokenizer tokenizer = new WordTokenizer();

    @Test
    void splitsTextIntoLowercaseWords() {
        List<String> words = tokenizer.tokenize("Hello world, hello!");

        assertEquals(List.of("hello", "world", "hello"), words);
    }

    @Test
    void ignoresRepeatedSeparators() {
        List<String> words = tokenizer.tokenize("Java,,,   Reactor!!");

        assertEquals(List.of("java", "reactor"), words);
    }
}