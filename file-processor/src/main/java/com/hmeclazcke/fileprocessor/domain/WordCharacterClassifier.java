package com.hmeclazcke.fileprocessor.domain;

public class WordCharacterClassifier {

    public boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character);
    }
}