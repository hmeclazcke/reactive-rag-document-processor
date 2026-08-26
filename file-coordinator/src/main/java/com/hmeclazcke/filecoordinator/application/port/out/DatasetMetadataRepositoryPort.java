package com.hmeclazcke.filecoordinator.application.port.out;

import com.hmeclazcke.filecoordinator.domain.ProcessingPlan;

import reactor.core.publisher.Mono;

public interface DatasetMetadataRepositoryPort {

    Mono<Void> save(ProcessingPlan plan);
}