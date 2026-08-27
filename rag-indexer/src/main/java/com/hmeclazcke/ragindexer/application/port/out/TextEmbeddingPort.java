package com.hmeclazcke.ragindexer.application.port.out;

import com.hmeclazcke.ragindexer.domain.TextEmbedding;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TextEmbeddingPort {

    Mono<List<TextEmbedding>> embedAll(List<String> texts);
}
