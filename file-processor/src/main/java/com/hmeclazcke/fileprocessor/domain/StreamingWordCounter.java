package com.hmeclazcke.fileprocessor.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StreamingWordCounter {

    private final WordCharacterClassifier characterClassifier = new WordCharacterClassifier();
    private final Map<String, WordFrequency> wordFrequencies = new HashMap<>();
    private final StringBuilder currentWord = new StringBuilder();

    public void acceptCodePoint(int codePoint) {
        if (isAsciiUppercaseLetter(codePoint)) {
            acceptNormalizedCodePoint(codePoint + ('a' - 'A'));
            return;
        }

        if (isAscii(codePoint)) {
            acceptNormalizedCodePoint(codePoint);
            return;
        }

        String normalizedText = new String(Character.toChars(codePoint)).toLowerCase(Locale.ROOT);
        normalizedText.codePoints().forEach(this::acceptNormalizedCodePoint);
    }

    private void acceptNormalizedCodePoint(int codePoint) {
        if (characterClassifier.isWordCodePoint(codePoint)) {
            currentWord.appendCodePoint(codePoint);
            return;
        }

        closeCurrentWord();
    }

    public Map<String, Long> result() {
        closeCurrentWord();

        Map<String, Long> result = new HashMap<>(wordFrequencies.size());
        wordFrequencies.forEach((word, frequency) -> result.put(word, frequency.count()));

        return Map.copyOf(result);
    }

    private void closeCurrentWord() {
        if (currentWord.isEmpty()) {
            return;
        }

        String word = currentWord.toString();
        WordFrequency frequency = wordFrequencies.get(word);

        if (frequency == null) {
            wordFrequencies.put(word, new WordFrequency());
        } else {
            frequency.increment();
        }

        currentWord.setLength(0);
    }

    private boolean isAscii(int codePoint) {
        return codePoint <= 0x7f;
    }

    private boolean isAsciiUppercaseLetter(int codePoint) {
        return codePoint >= 'A' && codePoint <= 'Z';
    }

    private static final class WordFrequency {

        private long count = 1;

        private void increment() {
            count++;
        }

        private long count() {
            return count;
        }
    }
}
