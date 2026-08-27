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
        assertTrue(classifier.isWordCharacter('\u00e1'));
        assertTrue(classifier.isWordCharacter('\u00f1'));
        assertTrue(classifier.isWordCodePoint('\u00c1'));
    }

    @Test
    void identifiesSeparators() {
        assertFalse(classifier.isWordCharacter(' '));
        assertFalse(classifier.isWordCharacter(','));
        assertFalse(classifier.isWordCharacter('.'));
        assertFalse(classifier.isWordCharacter('\n'));
    }
}
