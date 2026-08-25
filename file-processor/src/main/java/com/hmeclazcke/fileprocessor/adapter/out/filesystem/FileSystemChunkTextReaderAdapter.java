package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkTextReaderPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Reads line-delimited UTF-8 text from an assigned byte range.
 *
 * <p>Chunks use [start, end) byte offsets. Lines are the processing records.
 * This reader returns a line only when the line starts inside the assigned chunk.
 *
 * <p>If the chunk starts inside a line, that line is skipped. If a line starts
 * inside the chunk, the reader may read past endByteExclusive to finish it.
 *
 * <p>The reader finds line breaks before decoding, then decodes each complete
 * line as UTF-8 text.
 */
public class FileSystemChunkTextReaderAdapter implements ChunkTextReaderPort {

    private static final String READ_MODE = "r";
    private static final String COULD_NOT_READ_CHUNK_TEXT = "Could not read chunk text";
    private static final String COULD_NOT_DECODE_UTF_8_TEXT = "Could not decode UTF-8 text";
    private static final String LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH =
            "Line exceeds maximum supported length";

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

                // Flux.generate emits one complete line at a time, tied to downstream demand.
                // Lines are the processing records, so we do not split words or UTF-8 characters manually.
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

        private static final byte LINE_FEED = '\n';

        private final RandomAccessFile file;
        private final FileChunk chunk;

        private boolean initialized;
        private boolean completed;

        private ChunkTextFileReader(Path datasetPath, FileChunk chunk) throws IOException {
            this.file = new RandomAccessFile(datasetPath.toFile(), READ_MODE);
            this.chunk = chunk;
        }

        // Flux.generate calls this method when the next line is requested.
        // Sending at most one line through SynchronousSink keeps file reading tied to backpressure.
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
                moveToFirstOwnedLine();
                initialized = true;
            }

            if (completed || file.getFilePointer() >= chunk.endByteExclusive()) {
                completed = true;
                return null;
            }

            byte[] lineBytes = readLineBytes();

            if (lineBytes.length == 0) {
                completed = true;
                return null;
            }

            return decodeUtf8(lineBytes);
        }

        // Each line is owned by the chunk where that line starts.
        // If this chunk starts in the middle of a line, skip that partial line.
        private void moveToFirstOwnedLine() throws IOException {
            if (chunk.startByteInclusive() == 0) {
                file.seek(chunk.startByteInclusive());
                return;
            }

            file.seek(chunk.startByteInclusive() - 1);
            int previousByte = file.read();

            if (previousByte == LINE_FEED) {
                file.seek(chunk.startByteInclusive());
                return;
            }

            file.seek(chunk.startByteInclusive());
            skipUntilNextLineStart();
        }

        private void skipUntilNextLineStart() throws IOException {
            byte[] buffer = new byte[settings.bufferSizeBytes()];
            int skippedBytes = 0;

            while (true) {
                int bytesRead = file.read(buffer);

                if (bytesRead == -1) {
                    completed = true;
                    return;
                }

                int lineFeedIndex = lineFeedIndex(buffer, bytesRead);
                int bytesToSkip = lineFeedIndex == -1 ? bytesRead : lineFeedIndex + 1;

                skippedBytes += bytesToSkip;
                failIfLineIsTooLong(skippedBytes);

                if (lineFeedIndex != -1) {
                    seekBackUnreadBytes(bytesRead - bytesToSkip);
                    return;
                }
            }
        }

        // Reads one complete line, extending past the chunk end if needed.
        // This matches the contract that a line belongs to the chunk where it starts.
        private byte[] readLineBytes() throws IOException {
            ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[settings.bufferSizeBytes()];

            while (true) {
                int bytesRead = file.read(buffer);

                if (bytesRead == -1) {
                    return lineBytes.toByteArray();
                }

                int lineFeedIndex = lineFeedIndex(buffer, bytesRead);
                int bytesToKeep = lineFeedIndex == -1 ? bytesRead : lineFeedIndex + 1;

                appendLineBytes(lineBytes, buffer, bytesToKeep);

                if (lineFeedIndex != -1) {
                    seekBackUnreadBytes(bytesRead - bytesToKeep);
                    return lineBytes.toByteArray();
                }
            }
        }

        // Check the limit before storing more bytes so one huge line cannot grow memory without bounds.
        private void appendLineBytes(ByteArrayOutputStream lineBytes, byte[] buffer, int length) {
            if (lineBytes.size() + length > settings.maxLineLengthBytes()) {
                throw new IllegalStateException(LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
            }

            lineBytes.write(buffer, 0, length);
        }

        // Line breaks are detected at byte level because '\n' is the same single byte in UTF-8 and ASCII.
        private int lineFeedIndex(byte[] buffer, int bytesRead) {
            for (int index = 0; index < bytesRead; index++) {
                if (buffer[index] == LINE_FEED) {
                    return index;
                }
            }

            return -1;
        }

        // RandomAccessFile may read past the line break into the next line.
        // Move the pointer back so the next Flux request starts at the correct line.
        private void seekBackUnreadBytes(int unreadBytes) throws IOException {
            if (unreadBytes > 0) {
                file.seek(file.getFilePointer() - unreadBytes);
            }
        }

        // Decode only after a full line is collected, so Java never receives a cut UTF-8 character.
        private String decodeUtf8(byte[] bytes) {
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();
            } catch (CharacterCodingException exception) {
                throw new IllegalStateException(COULD_NOT_DECODE_UTF_8_TEXT, exception);
            }
        }

        private void failIfLineIsTooLong(int lineLengthBytes) {
            if (lineLengthBytes > settings.maxLineLengthBytes()) {
                throw new IllegalStateException(LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
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