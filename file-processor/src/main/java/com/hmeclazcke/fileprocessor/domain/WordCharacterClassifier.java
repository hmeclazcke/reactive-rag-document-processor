package com.hmeclazcke.fileprocessor.domain;

public class WordCharacterClassifier {

    public boolean isWordCharacter(char character) {
        return isLowercaseLetter(character) || isDigit(character);
    }

    private boolean isLowercaseLetter(char character) {
        return character >= 'a' && character <= 'z';
    }

    private boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }
}