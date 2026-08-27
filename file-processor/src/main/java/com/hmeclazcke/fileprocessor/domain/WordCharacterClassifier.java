package com.hmeclazcke.fileprocessor.domain;

public class WordCharacterClassifier {

    public boolean isWordCharacter(char character) {
        return isWordCodePoint(character);
    }

    public boolean isWordCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint);
    }
}
