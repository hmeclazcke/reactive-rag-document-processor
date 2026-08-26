package com.hmeclazcke.filegenerator.adapter.out.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiGeminiGeneratedSeedTextAdapterTest {

    @Test
    void returnsGeneratedSeedTextFromChatModel() {
        ChatModel chatModel = mock(ChatModel.class);
        SpringAiGeminiGeneratedSeedTextAdapter adapter =
                new SpringAiGeminiGeneratedSeedTextAdapter(chatModel);

        when(chatModel.call(anyString())).thenReturn("""
                java spring reactor
                mongo graphql rag
                """);

        String generatedText = adapter.generate();

        assertEquals("""
                java spring reactor
                mongo graphql rag
                """, generatedText);
        verify(chatModel).call(anyString());
    }
}