package com.hmeclazcke.filequeryapi.application.port.out;

import com.hmeclazcke.filequeryapi.domain.Dataset;
import reactor.core.publisher.Flux;

public interface DatasetQueryPort {

    Flux<Dataset> findAllDatasets();
}