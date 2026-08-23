package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.WordCharacterClassifier;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;

public class FileSystemChunkTextReaderAdapter implements ChunkTextReaderPort {

    private static final String READ_MODE = "r";
    private static final String COULD_NOT_READ_CHUNK_TEXT = "Could not read chunk text";
    private static final int DEFAULT_MAX_WORD_EXTENSION_BYTES = 1024 * 1024;
    private static final String WORD_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH = "Word exceeds maximum supported length";
    private final int maxWordExtensionBytes;

    private final WordCharacterClassifier characterClassifier = new WordCharacterClassifier();

    public FileSystemChunkTextReaderAdapter() {
        this(DEFAULT_MAX_WORD_EXTENSION_BYTES);
    }

    public FileSystemChunkTextReaderAdapter(int maxWordExtensionBytes) {
        this.maxWordExtensionBytes = maxWordExtensionBytes;
    }

    @Override
    public List<String> readText(Path datasetPath, FileChunk chunk) {
        try {
            return List.of(readChunkText(datasetPath, chunk));
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception);
        }
    }

    // Each word is counted by the chunk where that word starts.
    private String readChunkText(Path datasetPath, FileChunk chunk) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(datasetPath.toFile(), READ_MODE)) {

            // Jump directly to the assigned chunk start instead of reading the file from the beginning.
            file.seek(chunk.startByteInclusive());

            skipFirstWordFragmentIfNeeded(file, chunk);

            StringBuilder text = new StringBuilder();

            // First read the exact byte range assigned by the coordinator.
            readConfiguredRange(file, chunk, text);

            // If the range ended in the middle of a word, read a little further to finish it.
            completeLastWord(file, text);

            return text.toString();
        }
    }

    private void readConfiguredRange(RandomAccessFile file, FileChunk chunk, StringBuilder text) throws IOException {
        long bytesToRead = chunk.endByteExclusive() - chunk.startByteInclusive();

        for (long i = 0; i < bytesToRead; i++) {
            int nextByte = file.read();

            if (nextByte == -1) {
                return;
            }

            text.append((char) nextByte);
        }
    }

    private void completeLastWord(RandomAccessFile file, StringBuilder text) throws IOException {
        // Nothing to complete when the chunk is empty or already ends with a separator.
        if (text.isEmpty() || !characterClassifier.isWordCharacter(text.charAt(text.length() - 1))) {
            return;
        }

        int extendedBytes = 0;
        int nextByte = file.read();

        // Keep reading until the first separator or end of file.
        while (nextByte != -1 && characterClassifier.isWordCharacter((char) nextByte)) {

            // Protect the processor from malformed text with an extremely long word.
            if (extendedBytes >= maxWordExtensionBytes) {
                throw new IllegalStateException(WORD_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
            }

            text.append((char) nextByte);
            extendedBytes++;
            nextByte = file.read();
        }
    }

    private void skipFirstWordFragmentIfNeeded(RandomAccessFile file, FileChunk chunk) throws IOException {
        if (chunk.startByteInclusive() == 0) {
            return;
        }

        file.seek(chunk.startByteInclusive() - 1);
        int previousByte = file.read();
        int currentByte = file.read();

        if (!isMiddleOfWord(previousByte, currentByte)) {
            file.seek(chunk.startByteInclusive());
            return;
        }

        skipUntilSeparator(file);
    }

    private boolean isMiddleOfWord(int previousByte, int currentByte) {
        return previousByte != -1
                && currentByte != -1
                && characterClassifier.isWordCharacter((char) previousByte)
                && characterClassifier.isWordCharacter((char) currentByte);
    }

    private void skipUntilSeparator(RandomAccessFile file) throws IOException {
        int skippedBytes = 0;
        int nextByte = file.read();

        while (nextByte != -1 && characterClassifier.isWordCharacter((char) nextByte)) {
            // Protect the processor from malformed text with an extremely long word.
            if (skippedBytes >= maxWordExtensionBytes) {
                throw new IllegalStateException(WORD_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
            }

            skippedBytes++;
            nextByte = file.read();
        }
    }
}