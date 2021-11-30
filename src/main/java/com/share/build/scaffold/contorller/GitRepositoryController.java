package com.share.build.scaffold.contorller;

import com.share.build.scaffold.handler.GitRepositoryHandler;
import com.share.build.scaffold.model.CheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * 为了方便的使用 校验注解
 */
@RestController
@RequestMapping("/gitRepository/{id}")
@RequiredArgsConstructor
public class GitRepositoryController {
    private final GitRepositoryHandler gitRepositoryHandler;
    @PostMapping("/checkout")
    public Mono<ServerResponse> checkout(@PathVariable("id") Long id, @Valid @RequestBody Mono<CheckoutRequest> requestMono) {
        return gitRepositoryHandler.checkout(id,requestMono);
    }
    @GetMapping(path = "/localBranch",consumes = "application/json")
    public Mono<String> getLocalBranch(@PathVariable("id") Long id) {
        return gitRepositoryHandler.getLocalBranch(id);
    }
}
