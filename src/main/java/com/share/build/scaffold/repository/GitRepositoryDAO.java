package com.share.build.scaffold.repository;

import com.share.build.scaffold.model.bo.GitRepositoryBO;
import org.reactivestreams.Publisher;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GitRepositoryDAO extends ReactiveCrudRepository<GitRepositoryBO,Long> {
    Flux<GitRepositoryBO> findAllByAliasName(String name);
    Flux<GitRepositoryBO> findAllByAliasName(Publisher<String> name);


}
