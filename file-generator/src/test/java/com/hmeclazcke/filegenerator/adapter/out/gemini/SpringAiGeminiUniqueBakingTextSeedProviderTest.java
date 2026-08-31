package com.hmeclazcke.filegenerator.adapter.out.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiGeminiUniqueBakingTextSeedProviderTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final SpringAiGeminiUniqueBakingTextSeedProvider provider =
            new SpringAiGeminiUniqueBakingTextSeedProvider(chatModel);

    @Test
    void returnsGeneratedLinesAndRequestsAnotherBlockWhenBufferIsEmpty() {
        when(chatModel.call(anyString())).thenReturn(
                """
                        Strong bread flour helps sourdough keep gas during bulk fermentation.
                        Croissant butter should stay plastic during lamination.
                        """,
                """
                        Croissant butter should stay plastic during lamination.
                        Brioche dough needs enough kneading to support butter and eggs.
                        """
        );

        assertEquals(
                "Strong bread flour helps sourdough keep gas during bulk fermentation.\n",
                provider.nextLine()
        );
        assertEquals(
                "Croissant butter should stay plastic during lamination.\n",
                provider.nextLine()
        );
        assertEquals(
                "Brioche dough needs enough kneading to support butter and eggs.\n",
                provider.nextLine()
        );

        verify(chatModel, times(2)).call(anyString());
    }

    @Test
    void cleansSimpleListMarkersFromGeneratedLines() {
        when(chatModel.call(anyString())).thenReturn(
                """
                        1. Steam delays crust setting and helps oven spring in lean bread.
                        - Cold butter can shatter instead of stretching into pastry layers.
                        """
        );

        assertEquals(
                "Steam delays crust setting and helps oven spring in lean bread.\n",
                provider.nextLine()
        );
        assertEquals(
                "Cold butter can shatter instead of stretching into pastry layers.\n",
                provider.nextLine()
        );
    }

    @Test
    void failsWhenGeminiDoesNotReturnNewUniqueLines() {
        when(chatModel.call(anyString())).thenReturn(
                """
                        
                        """,
                """
                        
                        """,
                """
                        
                        """
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                provider::nextLine
        );

        assertEquals(
                "Gemini did not return new unique baking dataset lines",
                exception.getMessage()
        );
        verify(chatModel, times(3)).call(anyString());
    }
}
