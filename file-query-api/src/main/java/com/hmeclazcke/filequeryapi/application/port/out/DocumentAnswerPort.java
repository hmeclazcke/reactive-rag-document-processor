package com.hmeclazcke.filequeryapi.application.port.out;

import com.hmeclazcke.filequeryapi.domain.RagChunkSource;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentAnswerPort {

    Mono<String> answer(String question, List<RagChunkSource> contextSources);
}
