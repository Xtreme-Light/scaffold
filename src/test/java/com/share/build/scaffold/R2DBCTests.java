package com.share.build.scaffold;

import com.share.build.scaffold.repository.GitRepositoryDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,classes = ScaffoldApplication.class)
public class R2DBCTests {
    @Autowired
    GitRepositoryDAO repository;
    @Test
    void readsAllEntitiesCorrectly() {

        repository.findAll()
                .log()
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
    }
}
