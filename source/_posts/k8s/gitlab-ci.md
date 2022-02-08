---
title: 使用gitlab-ci自动构建代码
date: 2022-02-12 15:43:13
tags:
- kubernetes
- gitlab
---

## 使用docker 运行 gitlab runner


gitlab runner 有很多方式 当前我们只使用docker 的方式运行runner.
这个地方需要注意的是 公司使用的是自签名证书或自定义证书颁发机构所以在使用的过程中会遇到这个问题

```log
  Couldn't execute POST against https://hostname.tld/api/v4/jobs/request:
  Post https://hostname.tld/api/v4/jobs/request: x509: certificate signed by unknown authority
```

解决方法:

您可以使用openssl客户端将 GitLab 实例的证书下载到`/etc/gitlab-runner/certs`,因为使用的是docker 所以我们需要将证书挂载到
`/etc/gitlab-runner/certs`下

```bash
$ openssl s_client -showcerts -connect gitlab.example.com:443 < /dev/null 2>/dev/null | openssl x509 -outform PEM > /etc/gitlab-runner/certs/gitlab.example.com.crt
```

```bash
$ docker run -d --name gitlab-runner --restart always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /srv/gitlab-runner/config:/etc/gitlab-runner \
  -v /etc/gitlab-runner/certs:/etc/gitlab-runner/certs \
  gitlab/gitlab-runner:latest
```

查看日志发现缺少config.toml 文件
```log
$ docker logs -f gitlab-runner
Runtime platform                                    arch=amd64 os=linux pid=8 revision=98daeee0 version=14.7.0
Starting multi-runner from /etc/gitlab-runner/config.toml...  builds=0
Running in system-mode.

Configuration loaded                                builds=0
listen_address not defined, metrics & debug endpoints disabled  builds=0
[session_server].listen_address not defined, session endpoints disabled  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
ERROR: Failed to load config stat /etc/gitlab-runner/config.toml: no such file or directory  builds=0
```

解决方法:

直接执行 gitlab-runner register，并填写URL、token和描述即可，tags选填（参考设置tags）。
executor如果不知道怎么选，就选shell吧。 直接执行shell命令，简单有效。

执行完成后，gitlab-runner会自动修改/etc/gitlab-runner/config.toml文件，并重载daemon程序。

```bash
# 进入 gitlab-runner 容器
$ docker exec -it gitlab-runner

$ gitlab-ci-multi-runner register
root@10c9e0311f06:/# gitlab-ci-multi-runner register
Runtime platform                                    arch=amd64 os=linux pid=78 revision=98daeee0 version=14.7.0
Running in system-mode.

Enter the GitLab instance URL (for example, https://gitlab.com/):
https://gitlab.example.com/
Enter the registration token:
xxxxxxxxxxx
Enter a description for the runner:
[10c9e0311f06]: xxxxxxxx
Enter tags for the runner (comma-separated):
xx
Registering runner... succeeded                     runner=xxxxx
Enter an executor: kubernetes, docker-ssh, virtualbox, parallels, shell, ssh, docker+machine, docker-ssh+machine, custom, docker:
ssh
Enter the SSH server address (for example, my.server.com):
127.0.0.1
Enter the SSH server port (for example, 22):
22
Enter the SSH user (for example, root):
root
Enter the SSH password (for example, docker.io):
password
Enter the path to the SSH identity file (for example, /home/user/.ssh/id_rsa):

Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!

```

## 编写.gitlab-ci.yaml 文件

参考文档[.gitlab-ci 参考](https://docs.gitlab.com/ee/ci/yaml/)

把当前文件放在项目的根目录 `.gitlab-ci.yaml` 或者使用

例如:

```yaml
stages:
  - build
  - test

default:
  # 指定runner 的 tags
  tags:
    - demo

before_script:
  - echo "Hello"
variables:
  CI_COMMIT_REF_SLUG: "master"

cache:
  key: $CI_COMMIT_REF_SLUG
  paths:
    - vendor/    

job A:
  stage: build
  tags:
    - solarmesh-dev
  script:
    - mkdir -p vendor
    - echo "build" > vendor/hello.txt
  cache:
    key: $CI_COMMIT_REF_SLUG
    paths:
      - vendor/    
  after_script:
    - echo "World"

job B:
  stage: test
  # needs:
  #   - job A
  tags:
    - solarmesh-dev  
  cache:
    key: $CI_COMMIT_REF_SLUG
    paths:
      - vendor/
  script:
    - cat vendor/hello.txt

```