CREATE TABLE IF NOT EXISTS git_repository
(
    id LONG AUTO_INCREMENT,
    name VARCHAR(100) not null,
    url VARCHAR(100) not null,
    alias_name VARCHAR(100),
    local_path VARCHAR(100),
    username VARCHAR(100),
    git_password VARCHAR(100),
    description VARCHAR(255),
    update_time TIMESTAMP,
    create_time TIMESTAMP,
    constraint GIT_REPOSITORY_PK
        primary key (id)
);

comment on table git_repository is '各个git仓库配置信息';

comment on column git_repository.id is '主键';

comment on column git_repository.name is '仓库名称，默认从git url中获取';

comment on column git_repository.url is 'git仓库地址';

comment on column git_repository.alias_name is '别名，防止有的name产生冲突';

comment on column git_repository.local_path is 'clone后本地仓库的地址';

comment on column git_repository.username is 'clone这个项目的账号';

comment on column git_repository.git_password is 'git账号密码，如果为空，使用默认全局账号';
comment on column git_repository.description is '描述信息';

comment on column git_repository.update_time is '更新时间';

comment on column git_repository.create_time is '创建时间';

create unique index GIT_REPOSITORY_LOCAL_PATH_UK
    on git_repository (local_path);

create unique index GIT_REPOSITORY_URL_UK
    on git_repository (url);

