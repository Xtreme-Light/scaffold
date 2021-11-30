package com.share.build.scaffold.handler;

import com.share.build.scaffold.config.ScaffoldGitConfig;
import com.share.build.scaffold.model.CheckoutRequest;
import com.share.build.scaffold.model.GitRepositoryConfigRequest;
import com.share.build.scaffold.model.bo.GitRepositoryBO;
import com.share.build.scaffold.repository.GitRepositoryDAO;
import com.sun.istack.internal.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
@Slf4j
@RequiredArgsConstructor
public class GitRepositoryHandler {
    private final ScaffoldGitConfig scaffoldGitConfig;
    private final GitRepositoryDAO gitRepositoryDAO;
    private static final String GIT_DIRECTORY_NAME = ".git";

    public @NotNull
    Mono<ServerResponse> clone(ServerRequest request) {
        final String id = request.pathVariable("id");
        final long l = Long.parseLong(id);
        return clone(l).flatMap(v -> ServerResponse.ok().build());
    }

    public Mono<Void> clone(Long id) {
        final Mono<GitRepositoryBO> byId = gitRepositoryDAO.findById(id);
        return byId.flatMap(this::initGitRepository).log();
    }


    private Mono<Void> initGitRepository(GitRepositoryBO config) {
        final String basePath = scaffoldGitConfig.getBasePath();
        final String localPath = config.getLocalPath();
        if (StringUtils.hasLength(localPath)) {
            final Path path = Paths.get(localPath);
            if (Files.exists(path)
                    && Files.isDirectory(Paths.get(localPath))) {
                return Mono.from(
                                Mono.fromCallable(
                                        () -> Git.open(new File(localPath))
                                ).subscribeOn(Schedulers.boundedElastic()))
                        .flatMap(v -> Mono.empty());
            }
        }
        final String humanishName;
        try {
            humanishName = new URIish(config.getUrl()).getHumanishName();
        } catch (URISyntaxException e) {
            log.error("URI解析异常", e);
            return Mono.error(e);
        }
        final Path path = Paths.get(basePath);
        final File clonePath = new File(path.toAbsolutePath() + File.separator + humanishName);
        if (Files.exists(clonePath.toPath())) {
            if (!clonePath.delete()) {
                return Mono.error(new IOException("删除" + clonePath.getAbsolutePath() + "失败"));
            }
        }
        return Mono.using(() -> Git.cloneRepository()
                .setURI(config.getUrl())
                .setDirectory(clonePath)
                .setProgressMonitor(new SimpleProgressMonitor())
                .call(), Mono::just, $1 -> {
            log.info("成功创建仓库：{}", $1.getRepository().getDirectory());
            config.setLocalPath(clonePath.getAbsolutePath());
        }).then(gitRepositoryDAO.save(config).flatMap(v -> Mono.empty()));

    }

    public Mono<ServerResponse> listGitRepositories(ServerRequest request) {
        return ServerResponse.ok().body(BodyInserters.fromPublisher(gitRepositoryDAO.findAll(), GitRepositoryBO.class));
    }

    public Mono<Repository> consumingRepository(String path, Consumer<? super Repository> resourceCleanup) {
        // 基于资源，创建一个相关的MONO
        // 获取到Mono后进行相关的操作
        return Mono.using(() -> new FileRepositoryBuilder()
                        .readEnvironment() // scan up the file system tree
                        .findGitDir(new File(path)) // scan environment GIT_* variables
                        .build(),
                Mono::just, resourceCleanup);
    }

    public Mono<Repository> createNewRepository() {
        // prepare a new folder
        return Mono.from(
                Mono.fromCallable(() -> File.createTempFile("TestGitRepository", ""))
                        .subscribeOn(Schedulers.boundedElastic())
        ).doOnNext(v -> {
            if (!v.delete()) {
                Mono.error(new RuntimeException("无法删除文件" + v.getAbsolutePath()));
            }
        }).flatMap(
                v -> Mono.fromCallable(() -> FileRepositoryBuilder.create(new File(v, ".git")))
                        .subscribeOn(Schedulers.boundedElastic())
        ).doOnNext(v -> {
            try {
                v.create();
            } catch (IOException e) {
                log.error("IO异常", e);
                Mono.error(e);
            }
        });
    }

    public Mono<ServerResponse> listRemote(ServerRequest request) {
        final String id = request.pathVariable("id");
        final long l = Long.parseLong(id);
        return gitRepositoryDAO.findById(l)
                .flatMap(v -> {
                    try {
                        final Collection<Ref> call = Git.lsRemoteRepository()
                                .setHeads(true)
                                .setTags(true)
                                .setRemote(v.getUrl())
                                .call();
                        return Flux.fromIterable(call.stream().map(Ref::getName).collect(Collectors.toList()))
                                .collectList().flatMap(item ->
                                        ServerResponse.ok().bodyValue(item)
                                );
                    } catch (GitAPIException e) {
                        log.error("执行GitApi出现异常", e);
                        return Mono.error(e);
                    }
                }).onErrorResume(e ->
                        Mono.just(e).log().flatMap(throwable ->
                                {
                                    log.error("获取远端信息失败", e);
                                    return ServerResponse.badRequest().bodyValue("获取远端信息失败");
                                }
                        ));
    }

    public Mono<String> getLocalRepositoryCurrentBranch(Mono<Repository> repositoryMono) {
        return repositoryMono.map($1 -> {
            try {
                return $1.getBranch();
            } catch (IOException e) {
                log.error("获取本地仓库当前分支失败", e);
                throw new RuntimeException(e);
            }
        });
    }

    public Mono<ServerResponse> getRepositoryConfig(ServerRequest serverRequest) {
        final String id = serverRequest.pathVariable("id");
        final long l = Long.parseLong(id);
        return ServerResponse.ok().body(BodyInserters.fromPublisher(gitRepositoryDAO.findById(l), GitRepositoryBO.class));
    }

    public Mono<ServerResponse> saveGitRepositoryConfig(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(GitRepositoryConfigRequest.class)
                .flatMap(
                        v -> {
                            final GitRepositoryBO gitRepositoryBO = new GitRepositoryBO();
                            BeanUtils.copyProperties(v, gitRepositoryBO);
                            gitRepositoryBO.setUpdateTime(LocalDateTime.now());
                            if (v.getId() == null) {
                                gitRepositoryBO.setCreateTime(LocalDateTime.now());
                            }
                            return Mono.just(gitRepositoryBO);
                        }
                )
                .flatMap(gitRepositoryDAO::save)
                .flatMap(s -> ServerResponse.ok().bodyValue(s))
                .onErrorResume(e ->
                        Mono.just(e).log().flatMap(throwable ->
                                {
                                    log.error("保存数据失败", e);
                                    return ServerResponse.badRequest().bodyValue("保存数据失败");
                                }
                        )
                );

    }

    public Mono<ServerResponse> deleteRepositoryConfig(ServerRequest serverRequest) {
        final String id = serverRequest.pathVariable("id");
        final long l = Long.parseLong(id);
        return ServerResponse.ok().build(gitRepositoryDAO.deleteById(l).log()).log();
    }

    public Mono<ServerResponse> checkout(Long id, Mono<CheckoutRequest> serverRequest) {
        return serverRequest.doOnSuccess($1 -> gitRepositoryDAO.findById(id).subscribe($2 -> {
                    try (final Git open = Git.open(new File($2.getLocalPath()))) {
                        final Ref call = open.checkout()
                                .setCreateBranch(true)
                                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                                .setName($1.getBranch())
                                .setStartPoint("origin/" + $1.getBranch())
                                .call();
                        final String name = call.getName();
                        log.info("checkout 成功 {}", name);
                    } catch (IOException e) {
                        log.error("io异常", e);
                        Mono.error(e);
                    } catch (RefNotFoundException e) {
                        log.error("找不到对应的ref", e);
                    } catch (RefAlreadyExistsException e) {
                        log.error("ref 已经存在", e);
                        Mono.error(e);
                    } catch (InvalidRefNameException e) {
                        log.error("无效的refName", e);
                        Mono.error(e);
                    } catch (CheckoutConflictException e) {
                        log.error("git checkout 冲突", e);
                        Mono.error(e);
                    } catch (GitAPIException e) {
                        log.error("gitApi 操作异常", e);
                        Mono.error(e);
                    }
                }))
                .doOnError(throwable -> ServerResponse.badRequest().bodyValue(throwable.getMessage()))
                .flatMap($v -> ServerResponse.ok().build());
    }

    public Mono<ServerResponse> listBranches(ServerRequest serverRequest) {
        final String id = serverRequest.pathVariable("id");
        return gitRepositoryDAO.findById(Long.parseLong(id))
                .flatMap(v -> {
                    final String localPath = v.getLocalPath();
                    final String gitPath = localPath + File.separator + GIT_DIRECTORY_NAME;
                    try (final Git git = new Git(new FileRepository(gitPath))) {
                        final List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
                        final List<String> collect = call.stream().map(Ref::getName).collect(Collectors.toList());
                        log.info("收集到的为{}", collect);
                        return Mono.just(collect).log();
                    } catch (GitAPIException | IOException e) {
                        log.error("展示Branch时发生错误", e);
                        return Mono.error(e);
                    }
                })
                .flatMap(v -> ServerResponse.ok().bodyValue(v))
                .onErrorResume(error -> ServerResponse.badRequest().bodyValue(error));
    }

    public Mono<String> getLocalBranch(Long id) {
        return gitRepositoryDAO.findById(id)
                .flatMap($1 -> {
                    final String localPath = $1.getLocalPath();
                    return consumingRepository(localPath, Repository::close)
                            .mapNotNull($2 -> {
                                try {
                                    return $2.getBranch();
                                } catch (IOException e) {
                                    log.error("获取本地分支出现异常",e);
                                }
                                return null;
                            });
                });
    }


    private static class SimpleProgressMonitor implements ProgressMonitor {
        private int incrementWork;
//        private static final DecimalFormat DECIMALFORMAT = new DecimalFormat("##.%");

        @Override
        public void start(int totalTasks) {
            log.info("工作线程总数: {}", totalTasks);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            log.info("开始任务： {}: 工作量为{}", title, totalWork);
        }

        @Override
        public void update(int completed) {
            incrementWork += completed;
            log.info("完成工作量：{} ", incrementWork);
        }

        @Override
        public void endTask() {
            log.info("任务完成");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
