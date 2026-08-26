package com.hmeclazcke.filequeryapi.application;

import com.hmeclazcke.filequeryapi.application.port.out.DatasetQueryPort;
import com.hmeclazcke.filequeryapi.domain.Dataset;
import reactor.core.publisher.Flux;

public class ListDatasetsUseCase {

    private final DatasetQueryPort datasetQuery;

    public ListDatasetsUseCase(DatasetQueryPort datasetQuery) {
        this.datasetQuery = datasetQuery;
    }

    public Flux<Dataset> listDatasets() {
        return datasetQuery.findAllDatasets();
    }
}