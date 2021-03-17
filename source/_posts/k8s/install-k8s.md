---
title: 如何快速安装k8s集群
date: 2021-03-12 13:13:13
tags:
- kubernetes
---

有时候需要快速搭建一个k8s集群帮组我测试代码的功能，这样可以帮我省去很多时间，把更多的时间都投入到写代码中，我觉得这也是很有必要的。
接下来我就讲解如何快速搭建k8s集群。

首先准备若干个机器，这里我只是举例子 

我现在准备了 三台机器
```bash

10.10.13.113 master

10.10.13.114 node01

10.10.13.115 node02

```

1. 在ansible控制端配置免密码登录

```bash
# 或者传统 RSA 算法
$ ssh-keygen -t rsa -b 2048 -N '' -f ~/.ssh/id_rsa

$ ssh-copy-id root@$IPs #$IPs为所有节点地址包括自身，按照提示输入yes 和root密码

/usr/bin/ssh-copy-id: INFO: Source of key(s) to be installed: "/Users/clare/.ssh/id_rsa.pub"
/usr/bin/ssh-copy-id: INFO: attempting to log in with the new key(s), to filter out any that are already installed
/usr/bin/ssh-copy-id: INFO: 1 key(s) remain to be installed -- if you are prompted now it is to install the new keys
root@xxx.xxx.xxx.xxx's password:

Number of key(s) added:        1

Now try logging into the machine, with:   "ssh 'root@xxx.xxx.xxx.xxx'"
and check to make sure that only the key(s) you wanted were added.
```

```bash
# 测试是否可以免密登陆

$ ssh root@$IPs

```

3.1 安装ansible (也可以使用容器化运行kubeasz，已经预装好ansible)
```bash

# 注意pip 21.0以后不再支持python2和python3.5，需要如下安装
# To install pip for Python 2.7 install it from https://bootstrap.pypa.io/2.7/ :
curl -O https://bootstrap.pypa.io/2.7/get-pip.py
python get-pip.py
python -m pip install --upgrade "pip < 21.0"

# pip安装ansible(国内如果安装太慢可以直接用pip阿里云加速)
pip install ansible -i https://mirrors.aliyun.com/pypi/simple/
```

### 下载工具脚本`ezdown`

```bash
# 下载工具脚本ezdown，举例使用kubeasz版本3.0.0
$ export release=3.0.0

$ curl -C- -fLO --retry 3 https://github.com/easzlab/kubeasz/releases/download/${release}/ezdown

$ chmod +x ./ezdown

# 使用工具脚本下载
$ chmod +x ezdown

# 下载安装包
# k 指定kubernetes的版本
$ ./ezdown -D -k v1.18.3

```

4.2 创建集群配置实例

```bash

ezctl new k8s-01
2021-01-19 10:48:23 DEBUG generate custom cluster files in /etc/kubeasz/clusters/k8s-01
2021-01-19 10:48:23 DEBUG set version of common plugins
2021-01-19 10:48:23 DEBUG cluster k8s-01: files successfully created.
2021-01-19 10:48:23 INFO next steps 1: to config '/etc/kubeasz/clusters/k8s-01/hosts'
2021-01-19 10:48:23 INFO next steps 2: to config '/etc/kubeasz/clusters/k8s-01/config.yml'
然后根据提示配置'/etc/kubeasz/clusters/k8s-01/hosts' 和 '/etc/kubeasz/clusters/k8s-01/config.yml': 根据前面节点规划修改hosts 文件和其他集群层面的主要配置选项；其他集群组件等配置项可以在config.yml 文件中修改。
```

4.3 开始安装 如果你对集群安装流程不熟悉，请阅读项目首页 安装步骤 讲解后分步安装，并对 每步都进行验证

### 一键安装

```bash
$ ezctl setup k8s-01 all

# 或者分步安装，具体使用 ezctl help setup 查看分步安装帮助信息
# ezctl setup k8s-01 01
# ezctl setup k8s-01 02
# ezctl setup k8s-01 03
# ezctl setup k8s-01 04
```


