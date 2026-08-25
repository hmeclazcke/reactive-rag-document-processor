package com.hmeclazcke.filequeryapi.application.port.out;

import com.hmeclazcke.filequeryapi.domain.WordCount;
import reactor.core.publisher.Flux;

public interface WordCountQueryPort {

    Flux<WordCount> findTopWords(int limit);
}
