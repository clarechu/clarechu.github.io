---
title: Linux 虚拟网络设备详解之 Bridge 网桥
date: 2021-03-13 22:44:26
tags:
- linux
- network
---

同 tap/tun、veth-pair 一样，Bridge 也是一种虚拟网络设备，所以具备虚拟网络设备的所有特性，比如可以配置 IP、MAC 等。

除此之外，Bridge 还是一个交换机，具有交换机所有的功能。

对于普通的网络设备，就像一个管道，只有两端，数据从一端进，从另一端出。而 Bridge 有多个端口，数据可以从多个端口进，从多个端口出。

Bridge 的这个特性让它可以接入其他的网络设备.

使用 来操作linux Bridge

### 安装 `brctl`

```bash
$ yum install bridge-utils -y
```

我们模拟一个docker0 类似的网桥

1. 添加网桥(br0)
```bash
$ brctl addbr br0


# 方法一:
$ sudo ifconfig br0 192.168.100.1 netmask  255.255.255.0

# 方法二:

$ sudo ip addr add 192.168.100.0/16 dev bridge0

$ sudo ip link set dev bridge0 up

```

2.查看网桥

1）显示所有的网桥信息

```bash
$ sudo brctl show
```

2）显示某个网桥(br0)的信息

```bash
$ sudo brctl show br0
```

3.删除网桥(br0)

```bash
$ sudo brctl delbr br0
```


4. 将eth0端口加入网桥br0

```bash
$ brctl addif br0 eth0
```


5. 从网桥br0中删除eth0端口

```bash
$ brctl delif br0 eth0
```
