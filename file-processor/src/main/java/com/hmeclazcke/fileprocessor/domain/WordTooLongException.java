package com.hmeclazcke.fileprocessor.domain;

public class WordTooLongException extends RuntimeException {

    public WordTooLongException() {
        super("Word exceeds maximum supported length");
    }
}