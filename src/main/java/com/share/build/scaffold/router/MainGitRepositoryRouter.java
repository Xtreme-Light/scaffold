package com.share.build.scaffold.router;

import com.share.build.scaffold.handler.GitRepositoryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * 无法混合使用router和controller
 * https://github.com/spring-projects/spring-framework/issues/19968
 * 基线（主线）仓库管理
 * 1. 选择git仓库作为主线
 * 2. 选择对应git分支
 * 3. 选择git tag
 * 4. 识别该tag 或者branch下的特性功能
 * 5. 定制化选择特性功能
 * 4. 基于选择，去创建新的仓库，包括新的命名/数据库配置/中间件配置/私有化配置等等
 */
//@Configuration(proxyBeanMethods = false)
@Deprecated
public class MainGitRepositoryRouter {
//    @Bean
    public RouterFunction<ServerResponse> gitRepositoryRoute(GitRepositoryHandler gitRepository) {
        return RouterFunctions
                .nest(
                        path("/gitRepository")
                                .and(accept(MediaType.APPLICATION_JSON))
                                .and(contentType(MediaType.APPLICATION_JSON)),
                        route(
                                GET(""),
                                gitRepository::listGitRepositories
                        ).andRoute(
                                POST("").or(PUT("")),
                                gitRepository::saveGitRepositoryConfig
                        ))
                .andNest(
                        path("/gitRepository/{id}")
                                .and(accept(MediaType.APPLICATION_JSON))
                                .and(contentType(MediaType.APPLICATION_JSON)),
                        route(
                                POST("/listRemote"),
                                gitRepository::listRemote
                        ).andRoute(
                                POST("/clone"),
                                gitRepository::clone
                        ).andRoute(
                                DELETE(""),
                                gitRepository::deleteRepositoryConfig
                        ).andRoute(
                                GET(""),
                                gitRepository::getRepositoryConfig
                        ).andRoute(
                                GET("/branch"),
                                gitRepository::listBranches
                        )
                );
    }
}
