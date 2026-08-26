package com.hmeclazcke.filegenerator.adapter.out.llm;

import com.hmeclazcke.filegenerator.application.port.out.GeneratedSeedTextPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmTextSeedProviderTest {

    @Test
    void returnsGeneratedSeedLinesOneByOne() {
        FixedGeneratedSeedTextPort generatedSeedTextPort =
                new FixedGeneratedSeedTextPort("""
                        java spring reactor
                        
                        mongo graphql rag
                        """);

        LlmTextSeedProvider provider =
                new LlmTextSeedProvider(generatedSeedTextPort);

        assertEquals("java spring reactor\n", provider.nextLine());
        assertEquals("mongo graphql rag\n", provider.nextLine());
        assertEquals("java spring reactor\n", provider.nextLine());
    }

    @Test
    void failsWhenGeneratedTextDoesNotContainSeedLines() {
        FixedGeneratedSeedTextPort generatedSeedTextPort =
                new FixedGeneratedSeedTextPort("""
                        
                        
                        """);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new LlmTextSeedProvider(generatedSeedTextPort)
        );

        assertEquals(
                "generated seed text must contain at least one non-blank line",
                exception.getMessage()
        );
    }

    private record FixedGeneratedSeedTextPort(String generatedText)
            implements GeneratedSeedTextPort {

        @Override
        public String generate() {
            return generatedText;
        }
    }
}