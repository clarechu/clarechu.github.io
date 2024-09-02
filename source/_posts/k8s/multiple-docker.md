---
title: docker 多架构编译
date: 2024-09-02 20:18:13
tags:
- kubernetes
- docker
---

## 使用 buildx 构建多种系统架构支持的 Docker 镜像

在 Docker 19.03+ 版本中可以使用 $ docker buildx build 命令使用 BuildKit 构建镜像。
该命令支持 --platform 参数可以同时构建支持多种系统架构的 Docker 镜像，大大简化了构建步骤。

## 跨 CPU 架构编译程序的方法

通过 binfmt_misc 模拟目标硬件的用户空间
在 Linux 上,QEMU 除了可以模拟完整的操作系统之外,还有另外一种模式叫用户态模式（User mod）.该模式下 QEMU 将通过 binfmt_misc 在 Linux 内核中注册一个二进制转换处理程序, 并在程序运行时动态翻译二进制文件，根据需要将系统调用从目标 CPU 架构转换为当前系统的 CPU 架构。最终的效果看起来就像在本地运行目标 CPU 架构的二进制文件。
通过 QEMU 的用户态模式,我们可以创建轻量级的虚拟机（chroot 或容器）,然后在虚拟机系统中编译程序,和本地编译一样简单轻松.后面我们就会看到,跨平台构建 Docker 镜像用的就是这个方法.

## docker开启buildx插件

要在Docker CLI中启用实验性功能，需要编辑config.json文件，并将experimental设置为enabled

```bash
$ mkdir /etc/docker
$ cat > /etc/docker/daemon.json <<EOF
{
"experimental": true
}
EOF
```

验证当前docker Experimental 是否为`true`

```bash
$ docker version
Client:
 Cloud integration: v1.0.35+desktop.13
 Version:           26.1.1
 API version:       1.45
 Go version:        go1.21.9
 Git commit:        4cf5afa
 Built:             Tue Apr 30 11:46:57 2024
 OS/Arch:           linux/amd64
 Context:           default

Server: Docker Desktop
 Engine:
  Version:          26.1.1
  API version:      1.45 (minimum version 1.24)
  Go version:       go1.21.9
  Git commit:       ac2de55
  Built:            Tue Apr 30 11:48:28 2024
  OS/Arch:          linux/amd64
  Experimental:     true
 containerd:
  Version:          1.6.31
  GitCommit:        e377cd56a71523140ca6ae87e30244719194a521
 runc:
  Version:          1.1.12
  GitCommit:        v1.1.12-0-g51d5e94
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

###  创建buildx 实例
Docker for Linux 不支持构建 arm 架构镜像，我们可以运行一个新的容器让其支持该特性，Docker 桌面版无需进行此项设置。

```bash
$ docker buildx create --use --name=mybuilder-cn --driver docker-container --driver-opt image=dockerpracticesig/buildkit:master

$ docker buildx use mybuilder
```
查看docker 使用的buildx实例

```bash
$ docker buildx ls

NAME/NODE           DRIVER/ENDPOINT                   STATUS     BUILDKIT   PLATFORMS
mybuilder-cn*       docker-container
 \_ mybuilder-cn0    \_ unix:///var/run/docker.sock   inactive
default             docker
 \_ default          \_ default                       running    v0.13.2    linux/amd64, linux/amd64/v2, linux/amd64/v3, linux/arm64, linux/riscv64, linux/ppc64le, linux/s390x, linux/386, linux/mips64le, linux/mips64, linux/arm/v7, linux/arm/v6
desktop-linux                                         error

Cannot load builder desktop-linux: protocol not available

```

### 构建镜像

新建 Dockerfile 文件。

```Dockerfile
# 使用官方的基础镜像
FROM --platform=$TARGETPLATFORM ubuntu:20.04

# 设置环境变量
ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# 更新包列表并安装必要的依赖
RUN apt-get update && apt-get install -y curl build-essential

# 如果 需要对下载包设置代理 
RUN export HTTPS_PROXY=http://localhost:2181 && \
    apt-get update && apt-get install -y curl build-essential
```

使用 `$ docker buildx build` 命令构建镜像。

`--push` 参数表示将构建好的镜像推送到 Docker 仓库。或者使用`--load` image into docker use 

```bash
docker buildx build --platform linux/arm64,linux/amd64,linux/ppc64le -t ubuntu_multi_arch:20.04 .
```

### 帮助

1. 使用代理拉取镜像

在docker service 文件中添加环境变量 `Environment="HTTPS_PROXY=http://192.168.0.105:10811"`

```bash
查看docker services 文件

➜ systemctl status docker 
● docker.service - Docker Application Container Engine
     Loaded: loaded (/usr/lib/systemd/system/docker.service; enabled; preset: enabled)
     Active: active (running) since Mon 2024-09-02 21:09:52 CST; 4min 32s ago
TriggeredBy: ● docker.socket
       Docs: https://docs.docker.com
   Main PID: 7173 (dockerd)
      Tasks: 14
     Memory: 31.9M (peak: 37.1M)
        CPU: 569ms
     CGroup: /system.slice/docker.service
             └─7173 /usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock


### 设置service 环境变量
➜  sudo vim /usr/lib/systemd/system/docker.service

[Unit]
Description=Docker Application Container Engine
Documentation=https://docs.docker.com
After=network-online.target docker.socket firewalld.service containerd.service time-set.target
Wants=network-online.target containerd.service
Requires=docker.socket

[Service]
Type=notify
# the default is not to use systemd for cgroups because the delegate issues still
# exists and systemd currently does not support the cgroup feature set required
# for containers run by docker
ExecStart=/usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock
Environment="HTTPS_PROXY=http://192.168.0.105:10811"
ExecReload=/bin/kill -s HUP $MAINPID
TimeoutStartSec=0
RestartSec=2
Restart=always

```

重启docker 
```bash
➜  systemctl daemon-reload

➜  sudo systemctl restart docker

# 查看进程的环境变量

sudo cat /proc/8676/environ
LANG=zh_CN.UTF-8PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/snap/binNOTIFY_SOCKET=/run/systemd/notifyLISTEN_PID=8676LISTEN_FDS=1LISTEN_FDNAMES=docker.socketUSER=rootINVOCATION_ID=e88106aeaf814eb7b98546243332cb25JOURNAL_STREAM=8:60516SYSTEMD_EXEC_PID=8676MEMORY_PRESSURE_WATCH=/sys/fs/cgroup/system.slice/docker.service/memory.pressureMEMORY_PRESSURE_WRITE=c29tZSAyMDAwMDAgMjAwMDAwMAA=HTTPS_PROXY=http://192.168.0.105:10811%                           
```

测试拉取gcr中的镜像
```bash
➜ docker pull gcr.io/google-appengine/debian10:latest
latest: Pulling from google-appengine/debian10
Digest: sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0
Status: Image is up to date for gcr.io/google-appengine/debian10:latest
gcr.io/google-appengine/debian10:latest

```

如果是在docker buildx 中如果想使用buildx 拉取代理镜像的话

```bash
# 在创建 buildx builder 时，可以通过 --driver-opt 来设置代理。以下是一个示例：

$ docker buildx create --name mybuilder --driver docker-container \
  --driver-opt env.HTTP_PROXY=http://your-proxy-server:port \
  --driver-opt env.HTTPS_PROXY=https://your-proxy-server:port --use
  
## 例如:
$ docker buildx create --use --name=mybuilder-cn --driver docker-container \
  --driver-opt env.HTTPS_PROXY=http://192.168.0.105:10811 \
  --driver-opt image=dockerpracticesig/buildkit:master


```

测试可以使用buildx 拉取代理镜像
```bash
docker buildx build --platform linux/arm64,linux/amd64,linux/ppc64le -t ubuntu_multi_arch:20.04 . --push
[+] Building 16.3s (9/9) FINISHED                                                                                                                                                                                               docker-container:mybuilder-cn
 => [internal] load build definition from Dockerfile                                                                                                                                                                                                     0.0s
 => => transferring dockerfile: 211B                                                                                                                                                                                                                     0.0s
 => [internal] load .dockerignore                                                                                                                                                                                                                        0.0s
 => => transferring context: 2B                                                                                                                                                                                                                          0.0s
 => [linux/ppc64le internal] load metadata for gcr.io/google-appengine/debian10:latest                                                                                                                                                                   0.4s
 => [linux/amd64 internal] load metadata for gcr.io/google-appengine/debian10:latest                                                                                                                                                                     0.5s
 => [linux/arm64 internal] load metadata for gcr.io/google-appengine/debian10:latest                                                                                                                                                                     0.5s
 => [linux/ppc64le 1/1] FROM gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                    0.0s
 => => resolve gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                                  0.0s
 => [linux/arm64 1/1] FROM gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                      0.0s
 => => resolve gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                                  0.0s
 => [linux/amd64 1/1] FROM gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                      0.0s
 => => resolve gcr.io/google-appengine/debian10@sha256:e55f5ddf735dd5c74ac7c5042dcee0f6d6320522a44d6fc1461c895cfbffe7d0                                                                                                                                  0.0s
 => CANCELED exporting to image                                                                                                                                                                                                                         15.6s
 => => exporting layers                                                                                                                                                                                                                                  0.0s
 => => exporting manifest sha256:228c01d9de337fdd20588b8d8bd2ea2c9634a4990a83e8ea946cedc1f42eaa31                                                                                                                                                        0.0s
 => => exporting config sha256:34d27d38e46e9056d7ca761c5dec56a4a8b381d209fa5fdf7a5aa35b5215525b                                                                                                                                                          0.0s
 => => exporting manifest sha256:5a7c02d4989f4c77b60df162023f044c21bd4d4713a23b21f4b3b39e95d8f25e                                                                                                                                                        0.0s
 => => exporting config sha256:be4f0fe0a84c9698ae9dd663140080ce199372a9c044852b436a568de03381e7                                                                                                                                                          0.0s
 => => exporting manifest sha256:54b169b1c608bbca8a6c2105cf2c9a2713334dadd2e581fd8d6f532d5f4d3f93                                                                                                                                                        0.0s
 => => exporting config sha256:cf8540a1db8c83bab144ddcc8f88b69e7be2331d618937b904ee900a949b5296                                                                                                                                                          0.0s
 => => exporting manifest list sha256:c8c67560a97c12405a75c4fbb2f1a1fecee8e32f2af907bfac3b50b803f1ae49                                                                                                                                                   0.0s
 => => pushing layers                                                                                                                                                                                                                                   15.6s

```

