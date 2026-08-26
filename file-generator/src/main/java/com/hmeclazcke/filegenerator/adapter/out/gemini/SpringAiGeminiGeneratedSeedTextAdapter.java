package com.hmeclazcke.filegenerator.adapter.out.gemini;

import com.hmeclazcke.filegenerator.application.port.out.GeneratedSeedTextPort;
import org.springframework.ai.chat.model.ChatModel;

public class SpringAiGeminiGeneratedSeedTextAdapter implements GeneratedSeedTextPort {

    private static final String PROMPT = """
            Generate seed text lines for a large technical dataset.

            Rules:
            - Return only plain text.
            - Return one sentence per line.
            - Do not use numbering.
            - Do not use markdown.
            - Do not include explanations.
            - Use terms related to Java, Spring, Reactor, MongoDB, GraphQL, distributed processing, files, chunks, embeddings, and RAG.
            - Generate 100 lines.
            """;

    private final ChatModel chatModel;

    public SpringAiGeminiGeneratedSeedTextAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generate() {
        return chatModel.call(PROMPT);
    }
}