package com.hmeclazcke.fileprocessor.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WordCounter {

    private final WordTokenizer tokenizer;

    public WordCounter(WordTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Map<String, Long> count(List<String> textFragments) {
        return textFragments.stream()
                .flatMap(textFragment -> tokenizer.tokenize(textFragment).stream())
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.counting()
                ));
    }

    public Mono<Map<String, Long>> countReactive(Flux<String> textFragments) {
        return textFragments
                .flatMapIterable(tokenizer::tokenize)
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.counting()
                ));
    }
}