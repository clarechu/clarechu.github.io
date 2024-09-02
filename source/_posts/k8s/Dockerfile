# 使用官方的基础镜像
FROM ubuntu:20.04

# 设置环境变量
ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

# 更新包列表并安装必要的依赖
RUN apt-get update && apt-get install -y curl build-essential