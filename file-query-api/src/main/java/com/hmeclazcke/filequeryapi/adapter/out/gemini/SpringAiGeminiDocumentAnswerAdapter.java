package com.hmeclazcke.filequeryapi.adapter.out.gemini;

import com.hmeclazcke.filequeryapi.application.DocumentAnswerGenerationException;
import com.hmeclazcke.filequeryapi.application.port.out.DocumentAnswerPort;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class SpringAiGeminiDocumentAnswerAdapter implements DocumentAnswerPort {

    private static final String COULD_NOT_GENERATE_ANSWER =
            "Could not generate an answer with Gemini.";

    private static final String GEMINI_QUOTA_EXCEEDED =
            "Gemini quota exceeded while generating the answer.";

    private final ChatModel chatModel;

    public SpringAiGeminiDocumentAnswerAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Mono<String> answer(String question, List<RagChunkSource> contextSources) {
        // This is the generation step of RAG.
        // The LLM receives the user's question plus the source chunks recovered from MongoDB.
        String prompt = buildPrompt(question, contextSources);

        // Mono does not make this call non-blocking by itself.
        // Spring AI exposes ChatModel.call as a synchronous call, so it runs on boundedElastic.
        return Mono.fromSupplier(() -> chatModel.call(prompt))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(this::toDocumentAnswerGenerationException);
    }

    private String buildPrompt(String question, List<RagChunkSource> contextSources) {
        // Chunks arrive in similarity order, so the prompt shows the most relevant context first.
        String context = IntStream.range(0, contextSources.size())
                .mapToObj(index -> formatContextChunk(index + 1, contextSources.get(index)))
                .collect(java.util.stream.Collectors.joining("\n\n"));

        // The prompt tells Gemini to stay grounded in the retrieved document context.
        // That is what turns a normal LLM call into a RAG answer.
        return """
                You answer questions using only the retrieved document context.

                Rules:
                - Use the context below as your source of truth.
                - If the context does not contain enough information, say that the document context is not enough to answer.
                - Do not invent facts that are not supported by the context.
                - Keep the answer concise and clear.

                Question:
                %s

                Retrieved context:
                %s

                Answer:
                """.formatted(question, context);
    }

    private String formatContextChunk(int number, RagChunkSource source) {
        // The ids and byte ranges make each answer traceable back to the original dataset chunk.
        return """
                [chunk %d]
                ragChunkId: %s
                sourceChunkIndex: %d
                ragChunkIndex: %d
                byteRange: [%d, %d)
                text:
                %s
                """.formatted(
                number,
                source.ragChunkId(),
                source.sourceChunkIndex(),
                source.ragChunkIndex(),
                source.startByteInclusive(),
                source.endByteExclusive(),
                source.text()
        );
    }

    private DocumentAnswerGenerationException toDocumentAnswerGenerationException(Throwable error) {
        if (isQuotaExceeded(error)) {
            return new DocumentAnswerGenerationException(GEMINI_QUOTA_EXCEEDED, error);
        }

        return new DocumentAnswerGenerationException(COULD_NOT_GENERATE_ANSWER, error);
    }

    private boolean isQuotaExceeded(Throwable error) {
        Throwable current = error;

        while (current != null) {
            String message = current.getMessage();

            if (message != null && isQuotaMessage(message)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean isQuotaMessage(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);

        return normalizedMessage.contains("quota") || normalizedMessage.contains("429");
    }
}
