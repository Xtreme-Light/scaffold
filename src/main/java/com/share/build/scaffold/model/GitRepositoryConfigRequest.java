package com.share.build.scaffold.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitRepositoryConfigRequest {
    private Long id;
    private String name;
    private String url;
    private String aliasName;
    private String localPath;
    private String username;
    private String gitPassword;
    private String description;

}
