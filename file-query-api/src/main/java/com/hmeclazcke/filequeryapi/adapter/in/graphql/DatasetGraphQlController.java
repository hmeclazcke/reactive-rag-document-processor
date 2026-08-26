package com.hmeclazcke.filequeryapi.adapter.in.graphql;

import com.hmeclazcke.filequeryapi.application.ListDatasetsUseCase;
import com.hmeclazcke.filequeryapi.domain.Dataset;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class DatasetGraphQlController {

    private final ListDatasetsUseCase useCase;

    public DatasetGraphQlController(ListDatasetsUseCase useCase) {
        this.useCase = useCase;
    }

    @QueryMapping
    public Mono<List<Dataset>> datasets() {
        return useCase.listDatasets().collectList();
    }
}