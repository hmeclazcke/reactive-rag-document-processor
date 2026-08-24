package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.WordCharacterClassifier;
import com.hmeclazcke.fileprocessor.domain.WordTooLongException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class FileSystemChunkTextReaderAdapter implements ChunkTextReaderPort {

    private static final String READ_MODE = "r";
    private static final String COULD_NOT_READ_CHUNK_TEXT = "Could not read chunk text";

    private final WordCharacterClassifier characterClassifier = new WordCharacterClassifier();
    private final ChunkTextReaderSettings settings;

    public FileSystemChunkTextReaderAdapter(ChunkTextReaderSettings settings) {
        this.settings = settings;
    }

    @Override
    public Flux<String> readText(Path datasetPath, FileChunk chunk) {
        // Create the file-reading Flux only when someone subscribes to it.
        return Flux.defer(() -> {
            try {
                ChunkTextFileReader reader = new ChunkTextFileReader(datasetPath, chunk);

                // Flux.generate emits one fragment at a time, tied to downstream demand.
                // The internal buffer size controls each fragment size, so we do not load the whole chunk into memory.
                return Flux.<String>generate(reader::readNextFragment)
                        // Close the RandomAccessFile when the Flux completes, fails, or gets cancelled.
                        .doFinally(signalType -> reader.close());
            } catch (IOException exception) {
                return Flux.error(new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception));
            }
            // RandomAccessFile is blocking, so run this Flux on Reactor's boundedElastic thread pool.
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // One instance is created per readText subscription and keeps that read's file position/state.
    private final class ChunkTextFileReader {

        private final RandomAccessFile file;
        private final FileChunk chunk;

        // Keeps the beginning of a word when an internal buffer ends before that word is complete.
        private String carriedWordFragment = "";
        private boolean initialized;
        private boolean completed;

        private ChunkTextFileReader(Path datasetPath, FileChunk chunk) throws IOException {
            this.file = new RandomAccessFile(datasetPath.toFile(), READ_MODE);
            this.chunk = chunk;
        }

        private void readNextFragment(SynchronousSink<String> sink) {
            try {
                String fragment = readNextFragment();

                if (fragment == null) {
                    sink.complete();
                    return;
                }

                sink.next(fragment);
            } catch (IOException exception) {
                sink.error(new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception));
            } catch (RuntimeException exception) {
                sink.error(exception);
            }
        }

        private String readNextFragment() throws IOException {
            if (!initialized) {
                moveToFirstOwnedByte();
                initialized = true;
            }

            while (!completed) {
                String fragment = readNextFragmentCandidate();

                if (!fragment.isEmpty()) {
                    return fragment;
                }
            }

            return null;
        }

        private String readNextFragmentCandidate() throws IOException {
            if (file.getFilePointer() >= chunk.endByteExclusive()) {
                completed = true;
                return carriedWordFragment;
            }

            StringBuilder candidate = new StringBuilder(carriedWordFragment);
            carriedWordFragment = "";

            readUpToBufferOrChunkEnd(candidate);

            if (file.getFilePointer() >= chunk.endByteExclusive()) {
                completeLastWord(candidate);
                completed = true;
                return candidate.toString();
            }

            return splitCandidateWithoutCuttingWord(candidate);
        }

        // Each word is counted by the chunk where that word starts.
        private void moveToFirstOwnedByte() throws IOException {
            if (chunk.startByteInclusive() == 0) {
                file.seek(chunk.startByteInclusive());
                return;
            }

            file.seek(chunk.startByteInclusive() - 1);
            int previousByte = file.read();
            int currentByte = file.read();

            if (!isMiddleOfWord(previousByte, currentByte)) {
                file.seek(chunk.startByteInclusive());
                return;
            }

            file.seek(chunk.startByteInclusive());
            skipUntilSeparator();
        }

        private boolean isMiddleOfWord(int previousByte, int currentByte) {
            return previousByte != -1
                    && currentByte != -1
                    && characterClassifier.isWordCharacter((char) previousByte)
                    && characterClassifier.isWordCharacter((char) currentByte);
        }

        private void skipUntilSeparator() throws IOException {
            int skippedBytes = 0;
            int nextByte = file.read();

            while (nextByte != -1 && characterClassifier.isWordCharacter((char) nextByte)) {
                // Protect the processor from malformed text with an extremely long word.
                if (skippedBytes >= settings.maxWordLengthBytes()) {
                    throw new WordTooLongException();
                }

                skippedBytes++;
                nextByte = file.read();
            }
        }

        private void readUpToBufferOrChunkEnd(StringBuilder text) throws IOException {
            long remainingChunkBytes = chunk.endByteExclusive() - file.getFilePointer();
            long bytesToRead = Math.min(settings.bufferSizeBytes(), remainingChunkBytes);

            for (long i = 0; i < bytesToRead; i++) {
                int nextByte = file.read();

                if (nextByte == -1) {
                    completed = true;
                    return;
                }

                text.append((char) nextByte);
            }
        }

        private String splitCandidateWithoutCuttingWord(StringBuilder candidate) {
            int lastSeparatorIndex = lastSeparatorIndex(candidate);

            if (lastSeparatorIndex == -1) {
                carriedWordFragment = candidate.toString();
                failIfWordIsTooLong(carriedWordFragment.length());
                return "";
            }

            carriedWordFragment = candidate.substring(lastSeparatorIndex + 1);
            failIfWordIsTooLong(carriedWordFragment.length());

            return candidate.substring(0, lastSeparatorIndex + 1);
        }

        private int lastSeparatorIndex(StringBuilder text) {
            for (int index = text.length() - 1; index >= 0; index--) {
                if (!characterClassifier.isWordCharacter(text.charAt(index))) {
                    return index;
                }
            }

            return -1;
        }

        private void completeLastWord(StringBuilder text) throws IOException {
            int currentWordLength = trailingWordLength(text);

            if (currentWordLength == 0) {
                return;
            }

            failIfWordIsTooLong(currentWordLength);

            int nextByte = file.read();

            while (nextByte != -1 && characterClassifier.isWordCharacter((char) nextByte)) {
                // Read past the configured end only to finish the current word.
                currentWordLength++;
                failIfWordIsTooLong(currentWordLength);

                text.append((char) nextByte);
                nextByte = file.read();
            }
        }

        private int trailingWordLength(StringBuilder text) {
            int wordLength = 0;

            for (int index = text.length() - 1; index >= 0; index--) {
                if (!characterClassifier.isWordCharacter(text.charAt(index))) {
                    return wordLength;
                }

                wordLength++;
            }

            return wordLength;
        }

        private void failIfWordIsTooLong(int wordLength) {
            if (wordLength > settings.maxWordLengthBytes()) {
                throw new WordTooLongException();
            }
        }

        private void close() {
            try {
                file.close();
            } catch (IOException exception) {
                throw new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception);
            }
        }
    }
}