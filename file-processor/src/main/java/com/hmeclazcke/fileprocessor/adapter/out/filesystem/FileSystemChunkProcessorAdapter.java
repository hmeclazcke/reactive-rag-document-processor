package com.hmeclazcke.fileprocessor.adapter.out.filesystem;

import com.hmeclazcke.fileprocessor.application.port.out.FileChunkProcessorPort;
import com.hmeclazcke.fileprocessor.domain.ChunkWordCountsComputed;
import com.hmeclazcke.fileprocessor.domain.FileChunk;
import com.hmeclazcke.fileprocessor.domain.FileChunkProcessingEvent;
import com.hmeclazcke.fileprocessor.domain.RagChunk;
import com.hmeclazcke.fileprocessor.domain.RagChunkBatch;
import com.hmeclazcke.fileprocessor.domain.StreamingWordCounter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class FileSystemChunkProcessorAdapter implements FileChunkProcessorPort {

    private static final byte LINE_FEED = '\n';
    private static final String COULD_NOT_READ_CHUNK_TEXT = "Could not read chunk text";
    private static final String COULD_NOT_DECODE_UTF_8_TEXT = "Could not decode UTF-8 text";
    private static final String LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH =
            "Line exceeds maximum supported length";
    private static final String RAG_CHUNK_ID_SEPARATOR = ":";
    private static final String RAG_CHUNK_ID_TYPE = "rag";

    private final FileChunkProcessorSettings settings;

    public FileSystemChunkProcessorAdapter(FileChunkProcessorSettings settings) {
        this.settings = settings;
    }

    @Override
    public Flux<FileChunkProcessingEvent> process(String datasetId, Path datasetPath, FileChunk chunk) {
        return Flux.using(
                        () -> openProcessor(datasetId, datasetPath, chunk),
                        processor -> Flux.<FileChunkProcessingEvent>generate(sink -> {
                            try {
                                FileChunkProcessingEvent event = processor.nextEvent();

                                if (event == null) {
                                    sink.complete();
                                    return;
                                }

                                sink.next(event);
                            } catch (IOException exception) {
                                sink.error(new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception));
                            } catch (RuntimeException exception) {
                                sink.error(exception);
                            }
                        }),
                        ChunkFileProcessor::close
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ChunkFileProcessor openProcessor(String datasetId, Path datasetPath, FileChunk chunk) {
        try {
            FileChannel fileChannel = FileChannel.open(datasetPath, StandardOpenOption.READ);
            return new ChunkFileProcessor(datasetId, fileChannel, chunk);
        } catch (IOException exception) {
            throw new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception);
        }
    }

    private final class ChunkFileProcessor implements AutoCloseable {

        private final String datasetId;
        private final FileChannel fileChannel;
        private final FileChunk chunk;
        private final ByteBuffer buffer = ByteBuffer.allocate(settings.bufferSizeBytes());
        private final StreamingWordCounter wordCounter = new StreamingWordCounter();
        private final Utf8CodePointDecoder utf8Decoder = new Utf8CodePointDecoder(this::acceptCodePoint);
        private final StringBuilder currentLineText = new StringBuilder();
        private final StringBuilder currentRagChunkText = new StringBuilder();
        private final List<RagChunk> pendingRagChunks = new ArrayList<>();

        private long nextByteOffset;
        private long currentLineStartByte;
        private long currentRagChunkStartByte;
        private long currentRagChunkEndByte;
        private int currentLineLengthBytes;
        private int currentRagChunkTextLengthCharacters;
        private int nextRagChunkIndex;
        private boolean atLineStart = true;
        private boolean initialized;
        private boolean scanCompleted;
        private boolean scanFinalized;
        private boolean wordCountsEmitted;

        private ChunkFileProcessor(String datasetId, FileChannel fileChannel, FileChunk chunk) {
            this.datasetId = datasetId;
            this.fileChannel = fileChannel;
            this.chunk = chunk;
        }

        private FileChunkProcessingEvent nextEvent() throws IOException {
            initializeIfNeeded();

            FileChunkProcessingEvent pendingBatch = drainPendingRagChunkBatch();

            if (pendingBatch != null) {
                return pendingBatch;
            }

            while (!scanCompleted) {
                readNextBuffer();
                pendingBatch = drainPendingRagChunkBatch();

                if (pendingBatch != null) {
                    return pendingBatch;
                }
            }

            finalizeScanIfNeeded();
            pendingBatch = drainPendingRagChunkBatch();

            if (pendingBatch != null) {
                return pendingBatch;
            }

            if (!wordCountsEmitted) {
                wordCountsEmitted = true;
                // Word counts are emitted last because the frequency map is only final after the scan.
                return new ChunkWordCountsComputed(wordCounter.result());
            }

            return null;
        }

        private void initializeIfNeeded() throws IOException {
            if (initialized) {
                return;
            }

            moveToFirstOwnedLine();
            initialized = true;
        }

        // Each line is owned by the chunk where that line starts.
        // If this chunk starts inside a line, scan forward to the next line start before decoding anything.
        private void moveToFirstOwnedLine() throws IOException {
            nextByteOffset = firstOwnedLineStart();
            scanCompleted = nextByteOffset >= chunk.endByteExclusive();
            currentLineStartByte = nextByteOffset;
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
                scanCompleted = true;
                return;
            }

            buffer.clear();
            int bytesRead = fileChannel.read(buffer);

            if (bytesRead == -1) {
                scanCompleted = true;
                return;
            }

            processBuffer(bytesRead);
        }

        private void processBuffer(int bytesRead) {
            byte[] bytes = buffer.array();

            for (int index = 0; index < bytesRead && !scanCompleted; index++) {
                if (atLineStart && nextByteOffset >= chunk.endByteExclusive()) {
                    scanCompleted = true;
                    return;
                }

                byte value = bytes[index];

                nextByteOffset++;
                currentLineLengthBytes++;
                failIfLineIsTooLong(currentLineLengthBytes);
                utf8Decoder.accept(value);

                if (value == LINE_FEED) {
                    closeCurrentLine(nextByteOffset);
                    currentLineLengthBytes = 0;
                    atLineStart = true;
                    scanCompleted = nextByteOffset >= chunk.endByteExclusive();
                    continue;
                }

                atLineStart = false;
            }
        }

        private void acceptCodePoint(int codePoint) {
            // The same decoded code point feeds both outputs, keeping word counting and RAG extraction
            // on a single file read.
            wordCounter.acceptCodePoint(codePoint);
            currentLineText.appendCodePoint(codePoint);
        }

        private void finalizeScanIfNeeded() {
            if (scanFinalized) {
                return;
            }

            utf8Decoder.finish();
            closeCurrentLineAtEndOfFile();
            closeCurrentRagChunk();
            scanFinalized = true;
        }

        private void closeCurrentLineAtEndOfFile() {
            if (currentLineText.length() == 0) {
                return;
            }

            closeCurrentLine(nextByteOffset);
            currentLineLengthBytes = 0;
            atLineStart = true;
        }

        private void closeCurrentLine(long lineEndByteExclusive) {
            String lineText = currentLineText.toString();
            currentLineText.setLength(0);

            if (!lineText.isBlank()) {
                appendLineToRagChunk(lineText, currentLineStartByte, lineEndByteExclusive);
            }

            currentLineStartByte = lineEndByteExclusive;
        }

        private void appendLineToRagChunk(String lineText, long lineStartByte, long lineEndByteExclusive) {
            int lineTextLengthCharacters = lineText.codePointCount(0, lineText.length());

            // RAG chunks are split between full lines only. A single line may exceed the configured
            // target size, but it is still kept whole to avoid cutting sentence text.
            if (currentRagChunkText.length() > 0
                    && currentRagChunkTextLengthCharacters + lineTextLengthCharacters
                    > settings.ragChunkMaxTextLengthCharacters()) {
                closeCurrentRagChunk();
            }

            if (currentRagChunkText.length() == 0) {
                currentRagChunkStartByte = lineStartByte;
            }

            currentRagChunkText.append(lineText);
            currentRagChunkTextLengthCharacters += lineTextLengthCharacters;
            currentRagChunkEndByte = lineEndByteExclusive;
        }

        private void closeCurrentRagChunk() {
            if (currentRagChunkText.length() == 0) {
                return;
            }

            pendingRagChunks.add(new RagChunk(
                    ragChunkId(nextRagChunkIndex),
                    datasetId,
                    chunk.index(),
                    nextRagChunkIndex,
                    currentRagChunkText.toString(),
                    currentRagChunkStartByte,
                    currentRagChunkEndByte
            ));

            nextRagChunkIndex++;
            currentRagChunkText.setLength(0);
            currentRagChunkTextLengthCharacters = 0;
            currentRagChunkStartByte = 0;
            currentRagChunkEndByte = 0;
        }

        private FileChunkProcessingEvent drainPendingRagChunkBatch() {
            if (pendingRagChunks.isEmpty()) {
                return null;
            }

            // During the scan, wait for a full batch to avoid very small Mongo writes. At the end,
            // flush whatever is left.
            if (!scanCompleted && pendingRagChunks.size() < settings.ragChunkBatchSize()) {
                return null;
            }

            int batchSize = Math.min(settings.ragChunkBatchSize(), pendingRagChunks.size());
            List<RagChunk> batch = List.copyOf(pendingRagChunks.subList(0, batchSize));
            pendingRagChunks.subList(0, batchSize).clear();

            return new RagChunkBatch(batch);
        }

        private String ragChunkId(int ragChunkIndex) {
            return datasetId
                    + RAG_CHUNK_ID_SEPARATOR
                    + RAG_CHUNK_ID_TYPE
                    + RAG_CHUNK_ID_SEPARATOR
                    + chunk.index()
                    + RAG_CHUNK_ID_SEPARATOR
                    + ragChunkIndex;
        }

        private void failIfLineIsTooLong(int lineLengthBytes) {
            if (lineLengthBytes > settings.maxLineLengthBytes()) {
                throw new IllegalStateException(LINE_EXCEEDS_MAXIMUM_SUPPORTED_LENGTH);
            }
        }

        @Override
        public void close() {
            try {
                fileChannel.close();
            } catch (IOException exception) {
                throw new IllegalStateException(COULD_NOT_READ_CHUNK_TEXT, exception);
            }
        }
    }

    private static final class Utf8CodePointDecoder {

        private final IntConsumer codePointConsumer;

        private int codePoint;
        private int expectedContinuationBytes;
        private int minimumCodePoint;

        private Utf8CodePointDecoder(IntConsumer codePointConsumer) {
            this.codePointConsumer = codePointConsumer;
        }

        // Decode UTF-8 incrementally because buffers may split a multi-byte character.
        // Newline ownership is decided from bytes, but text processing is done from decoded Unicode code points.
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
                codePointConsumer.accept(unsignedByte);
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

            codePointConsumer.accept(codePoint);
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
