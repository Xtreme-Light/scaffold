package com.share.build.scaffold.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConfigurationProperties(prefix = "scaffold.config.git")
@Data
@Slf4j
public class ScaffoldGitConfig {
    private String basePath;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasLength(basePath)) {
            log.debug("没有配置git基础路径，将使用默认路径");
            try {
                final Path tempScaffoldDirectory = Files.createTempDirectory("_TempScaffoldDirectory");
                basePath = tempScaffoldDirectory.toAbsolutePath().toString();
                if (!Files.exists(tempScaffoldDirectory)) {
                    Files.createDirectory(tempScaffoldDirectory);
                }
            } catch (IOException e) {
                log.error("初始化git基础路径失败",e);
                throw new RuntimeException("初始化git基础路径失败");
            }
        }
    }

}
