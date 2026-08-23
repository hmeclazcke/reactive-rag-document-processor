package com.hmeclazcke.fileprocessor.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WordTokenizer {

    private final WordCharacterClassifier characterClassifier = new WordCharacterClassifier();

    public List<String> tokenize(String text) {
        String normalizedText = text.toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();

        // Build each word character by character and close it when a separator appears.
        for (char character : normalizedText.toCharArray()) {
            if (characterClassifier.isWordCharacter(character)) {
                currentWord.append(character);
            } else if (currentWord.length() > 0) {
                words.add(currentWord.toString());
                currentWord.setLength(0);
            }
        }

        if (currentWord.length() > 0) {
            words.add(currentWord.toString());
        }

        return words;
    }
}