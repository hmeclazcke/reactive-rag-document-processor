package com.hmeclazcke.fileprocessor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordCharacterClassifierTest {

    private final WordCharacterClassifier classifier = new WordCharacterClassifier();

    @Test
    void identifiesWordCharacters() {
        assertTrue(classifier.isWordCharacter('a'));
        assertTrue(classifier.isWordCharacter('z'));
        assertTrue(classifier.isWordCharacter('0'));
        assertTrue(classifier.isWordCharacter('9'));
        assertTrue(classifier.isWordCharacter('A'));
        assertTrue(classifier.isWordCharacter('Z'));
        assertTrue(classifier.isWordCharacter('á'));
        assertTrue(classifier.isWordCharacter('ñ'));
        assertTrue(classifier.isWordCharacter('İ'));
    }

    @Test
    void identifiesSeparators() {
        assertFalse(classifier.isWordCharacter(' '));
        assertFalse(classifier.isWordCharacter(','));
        assertFalse(classifier.isWordCharacter('.'));
        assertFalse(classifier.isWordCharacter('\n'));
    }
}