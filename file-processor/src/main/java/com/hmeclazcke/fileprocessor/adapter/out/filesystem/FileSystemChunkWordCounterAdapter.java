package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.application.port.out.ChunkWordCounterPort;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.StreamingWordCounter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class FileSystemChunkWordCounterAdapter implements ChunkWordCounterPort {

    private static final byte LINE_FEED = '\n';
    private static final String COULD_NOT_READ_CHUNK_TEXT = "Could not read chunk text";
    private static final String COULD_NOT_DECODE_UTF_8_TEXT = "Could not decode UTF-8 text";
    private static final String LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH =
            "Line exceeds maximum supported length";

    private final ChunkWordCounterSettings settings;

    public FileSystemChunkWordCounterAdapter(ChunkWordCounterSettings settings) {
        this.settings = settings;
    }

    @Override
    public Mono<Map<String, Long>> countWords(Path datasetPath, FileChunk chunk) {
        return Mono.fromCallable(() -> countWordsBlocking(datasetPath, chunk))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Map<String, Long> countWordsBlocking(Path datasetPath, FileChunk chunk) {
        try (FileChannel fileChannel = FileChannel.open(datasetPath, StandardOpenOption.READ)) {
            return new ChunkFileWordCounter(fileChannel, chunk).countWords();
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception);
        }
    }

    private final class ChunkFileWordCounter {

        private final FileChannel fileChannel;
        private final FileChunk chunk;
        private final ByteBuffer buffer = ByteBuffer.allocate(settings.bufferSizeBytes());
        private final StreamingWordCounter wordCounter = new StreamingWordCounter();
        private final Utf8CodePointDecoder utf8Decoder = new Utf8CodePointDecoder(wordCounter);

        private long nextByteOffset;
        private int currentLineLengthBytes;
        private boolean atLineStart = true;
        private boolean completed;

        private ChunkFileWordCounter(FileChannel fileChannel, FileChunk chunk) {
            this.fileChannel = fileChannel;
            this.chunk = chunk;
        }

        private Map<String, Long> countWords() throws IOException {
            moveToFirstOwnedLine();

            while (!completed) {
                readNextBuffer();
            }

            utf8Decoder.finish();
            return wordCounter.result();
        }

        // Each line is owned by the chunk where that line starts.
        // If this chunk starts inside a line, scan forward to the next line start before decoding anything.
        private void moveToFirstOwnedLine() throws IOException {
            nextByteOffset = firstOwnedLineStart();
            completed = nextByteOffset >= chunk.endByteExclusive();
            fileChannel.position(nextByteOffset);
        }

        private long firstOwnedLineStart() throws IOException {
            if (chunk.startByteInclusive() == 0) {
                return chunk.startByteInclusive();
            }

            int previousByte = readByteAt(chunk.startByteInclusive() - 1);

            if (previousByte == LINE_FEED) {
                return chunk.startByteInclusive();
            }

            return skipPartialLine(chunk.startByteInclusive());
        }

        private int readByteAt(long position) throws IOException {
            ByteBuffer oneByteBuffer = ByteBuffer.allocate(1);
            int bytesRead = fileChannel.read(oneByteBuffer, position);

            if (bytesRead == -1) {
                return -1;
            }

            oneByteBuffer.flip();
            return oneByteBuffer.get() & 0xff;
        }

        private long skipPartialLine(long startOffset) throws IOException {
            ByteBuffer skipBuffer = ByteBuffer.allocate(settings.bufferSizeBytes());
            long offset = startOffset;
            int skippedLineBytes = 0;

            fileChannel.position(startOffset);

            while (true) {
                skipBuffer.clear();
                int bytesRead = fileChannel.read(skipBuffer);

                if (bytesRead == -1) {
                    return offset;
                }

                byte[] bytes = skipBuffer.array();

                for (int index = 0; index < bytesRead; index++) {
                    byte value = bytes[index];
                    offset++;
                    skippedLineBytes++;
                    failIfLineIsTooLong(skippedLineBytes);

                    if (value == LINE_FEED) {
                        return offset;
                    }
                }
            }
        }

        private void readNextBuffer() throws IOException {
            if (atLineStart && nextByteOffset >= chunk.endByteExclusive()) {
                completed = true;
                return;
            }

            buffer.clear();
            int bytesRead = fileChannel.read(buffer);

            if (bytesRead == -1) {
                completed = true;
                return;
            }

            processBuffer(bytesRead);
        }

        private void processBuffer(int bytesRead) {
            byte[] bytes = buffer.array();

            for (int index = 0; index < bytesRead && !completed; index++) {
                if (atLineStart && nextByteOffset >= chunk.endByteExclusive()) {
                    completed = true;
                    return;
                }

                byte value = bytes[index];

                nextByteOffset++;
                currentLineLengthBytes++;
                failIfLineIsTooLong(currentLineLengthBytes);
                utf8Decoder.accept(value);

                if (value == LINE_FEED) {
                    currentLineLengthBytes = 0;
                    atLineStart = true;
                    completed = nextByteOffset >= chunk.endByteExclusive();
                    continue;
                }

                atLineStart = false;
            }
        }

        private void failIfLineIsTooLong(int lineLengthBytes) {
            if (lineLengthBytes > settings.maxLineLengthBytes()) {
                throw new IllegalStateException(LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
            }
        }
    }

    private static final class Utf8CodePointDecoder {

        private final StreamingWordCounter wordCounter;

        private int codePoint;
        private int expectedContinuationBytes;
        private int minimumCodePoint;

        private Utf8CodePointDecoder(StreamingWordCounter wordCounter) {
            this.wordCounter = wordCounter;
        }

        // Decode UTF-8 incrementally because buffers may split a multi-byte character.
        // Newline ownership is decided from bytes, but word counting is done from decoded Unicode code points.
        private void accept(byte value) {
            int unsignedByte = value & 0xff;

            if (expectedContinuationBytes == 0) {
                acceptLeadingByte(unsignedByte);
                return;
            }

            acceptContinuationByte(unsignedByte);
        }

        private void acceptLeadingByte(int unsignedByte) {
            if (unsignedByte <= 0x7f) {
                wordCounter.acceptCodePoint(unsignedByte);
                return;
            }

            if (unsignedByte >= 0xc2 && unsignedByte <= 0xdf) {
                startMultiByteCodePoint(unsignedByte & 0x1f, 1, 0x80);
                return;
            }

            if (unsignedByte >= 0xe0 && unsignedByte <= 0xef) {
                startMultiByteCodePoint(unsignedByte & 0x0f, 2, 0x800);
                return;
            }

            if (unsignedByte >= 0xf0 && unsignedByte <= 0xf4) {
                startMultiByteCodePoint(unsignedByte & 0x07, 3, 0x10000);
                return;
            }

            throwInvalidUtf8();
        }

        private void startMultiByteCodePoint(int initialCodePoint, int continuationBytes, int minimumCodePoint) {
            this.codePoint = initialCodePoint;
            this.expectedContinuationBytes = continuationBytes;
            this.minimumCodePoint = minimumCodePoint;
        }

        private void acceptContinuationByte(int unsignedByte) {
            if ((unsignedByte & 0xc0) != 0x80) {
                throwInvalidUtf8();
            }

            codePoint = (codePoint << 6) | (unsignedByte & 0x3f);
            expectedContinuationBytes--;

            if (expectedContinuationBytes > 0) {
                return;
            }

            if (codePoint < minimumCodePoint
                    || codePoint > Character.MAX_CODE_POINT
                    || isSurrogateCodePoint(codePoint)) {
                throwInvalidUtf8();
            }

            wordCounter.acceptCodePoint(codePoint);
        }

        private void finish() {
            if (expectedContinuationBytes != 0) {
                throwInvalidUtf8();
            }
        }

        private boolean isSurrogateCodePoint(int codePoint) {
            return codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE;
        }

        private void throwInvalidUtf8() {
            throw new IllegalStateException(COULD_NOT_DECODE_UTF_8_TEXT);
        }
    }
}
