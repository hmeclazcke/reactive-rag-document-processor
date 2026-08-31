package com.hmeclazcke.filequeryapi.adapter.out.gemini;

import com.hmeclazcke.filequeryapi.application.DocumentAnswerGenerationException;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiGeminiDocumentAnswerAdapterTest {

    private static final String QUESTION = "How does the processor create RAG chunks?";

    private final ChatModel chatModel = mock(ChatModel.class);
    private final SpringAiGeminiDocumentAnswerAdapter adapter =
            new SpringAiGeminiDocumentAnswerAdapter(chatModel);

    @Test
    void sendsQuestionAndContextSourcesToChatModel() {
        RagChunkSource source = new RagChunkSource(
                1,
                "dataset-1g-gemini:rag:0:1",
                0,
                1,
                100,
                180,
                "The processor accumulates complete lines into RAG chunks."
        );

        when(chatModel.call(anyString())).thenReturn(
                "The processor creates RAG chunks by accumulating complete lines."
        );

        StepVerifier.create(adapter.answer(QUESTION, List.of(source)))
                .expectNext("The processor creates RAG chunks by accumulating complete lines.")
                .verifyComplete();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains(QUESTION));
        assertTrue(prompt.contains(source.ragChunkId()));
        assertTrue(prompt.contains(source.text()));
        assertTrue(prompt.contains("Use the context below as your source of truth."));
    }

    @Test
    void returnsClearErrorWhenGeminiQuotaIsExceeded() {
        RagChunkSource source = new RagChunkSource(
                1,
                "dataset-1g-gemini:rag:0:1",
                0,
                1,
                100,
                180,
                "The processor accumulates complete lines into RAG chunks."
        );

        when(chatModel.call(anyString())).thenThrow(new RuntimeException(
                "Failed to generate content",
                new RuntimeException("429 quota exceeded")
        ));

        StepVerifier.create(adapter.answer(QUESTION, List.of(source)))
                .expectErrorMatches(error ->
                        error instanceof DocumentAnswerGenerationException
                                && error.getMessage().equals("Gemini quota exceeded while generating the answer.")
                )
                .verify();
    }
}
