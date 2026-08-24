package com.hmeclazcke.fileprocessor.domain;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordCounterTest {

    private final WordTokenizer tokenizer = new WordTokenizer();
    private final WordCounter wordCounter = new WordCounter(tokenizer);

    @Test
    void countsWordsFromLines() {
        List<String> lines = List.of(
                "hello world",
                "hello reactor"
        );

        Map<String, Long> counts = wordCounter.count(lines);

        assertEquals(Map.of(
                "hello", 2L,
                "world", 1L,
                "reactor", 1L
        ), counts);
    }

    @Test
    void countsNormalizedWords() {
        List<String> lines = List.of(
                "Java, java!",
                "JAVA"
        );

        Map<String, Long> counts = wordCounter.count(lines);

        assertEquals(Map.of(
                "java", 3L
        ), counts);
    }

    @Test
    void countsWordsFromTextFragmentFlux() {
        Flux<String> textFragments = Flux.just(
                "hello world",
                "hello reactor"
        );

        StepVerifier.create(wordCounter.countReactive(textFragments))
                .expectNext(Map.of(
                        "hello", 2L,
                        "world", 1L,
                        "reactor", 1L
                ))
                .verifyComplete();
    }
}