package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.WordCountQueryPort;
import com.hmeclazcke.filequeryapi.domain.WordCount;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetTopWordsUseCaseTest {

    private final WordCountQueryPort wordCountQuery = mock(WordCountQueryPort.class);
    private final GetTopWordsUseCase useCase = new GetTopWordsUseCase(wordCountQuery);

    @Test
    void returnsTopWordsFromQueryPort() {
        when(wordCountQuery.findTopWords(2)).thenReturn(Flux.just(
                new WordCount("java", 5),
                new WordCount("reactor", 3)
        ));

        StepVerifier.create(useCase.getTopWords(2))
                .expectNext(
                        new WordCount("java", 5),
                        new WordCount("reactor", 3)
                )
                .verifyComplete();

        verify(wordCountQuery).findTopWords(2);
    }

    @Test
    void failsWhenLimitIsZero() {
        StepVerifier.create(useCase.getTopWords(0))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void failsWhenLimitIsNegative() {
        StepVerifier.create(useCase.getTopWords(-1))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}