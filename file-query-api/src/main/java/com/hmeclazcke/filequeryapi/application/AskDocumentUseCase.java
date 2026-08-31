package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.DocumentAnswerPort;
import com.hmeclazcke.filequeryapi.domain.DocumentAnswer;
import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import reactor.core.publisher.Mono;

import java.util.List;

public class AskDocumentUseCase {

    private static final DocumentAnswer NO_RELEVANT_CONTEXT_ANSWER =
            new DocumentAnswer("No relevant context was found for this question.");

    private final SearchDocumentContextUseCase searchDocumentContext;
    private final DocumentAnswerPort documentAnswer;

    public AskDocumentUseCase(
            SearchDocumentContextUseCase searchDocumentContext,
            DocumentAnswerPort documentAnswer
    ) {
        this.searchDocumentContext = searchDocumentContext;
        this.documentAnswer = documentAnswer;
    }

    public Mono<DocumentAnswer> ask(String datasetId, String question) {
        return searchDocumentContext.search(datasetId, question)
                .flatMap(contextSources -> answer(question, contextSources));
    }

    private Mono<DocumentAnswer> answer(String question, List<RagChunkSource> contextSources) {
        if (contextSources.isEmpty()) {
            // Without recovered source text, the LLM would have nothing grounded to answer from.
            return Mono.just(NO_RELEVANT_CONTEXT_ANSWER);
        }

        // Here the question and the recovered chunks go to the LLM.
        // The adapter builds the prompt and performs the concrete provider call.
        return documentAnswer.answer(question, contextSources)
                .map(answer -> new DocumentAnswer(answer, contextSources));
    }
}
