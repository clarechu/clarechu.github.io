---
title: 更改docker默认挂载目录
---
更改docker默认挂载目录

## 场景

生产上运行了一段时间docker后，根分区使用量报警，由于根分区不是lvm类型的，所以无法做扩容，故采用新加一块硬盘，挂载到新目录/docker/，并将docker默认挂载目录改到这个目录的方法解决磁盘将满的问题。

### 新加磁盘并挂载

```bash
fdisk /dev/vdb


过程略
mkfs.xfs /dev/vdb1
mkdir /docker
mount /dev/vdc1 /docker
```

```bash
[root@cloud-jumpserver01 ~]# blkid 
/dev/vdc1: UUID="27d703ee-41b1-4b7e-860b-aa465b807e39" TYPE="xfs" 
cat /etc/fstab 
UUID=27d703ee-41b1-4b7e-860b-aa465b807e39 /dev/vdc1               xfs     defaults        1 1
```

## 更改docker挂载目录

```bash
cat /etc/docker/daemon.json 
{"registry-mirrors": ["http://f1361db2.m.daocloud.io"],
"data-root": "/docker"
}
```

## 加载配置

```bash
systemctl daemon-reload
systemctl restart docker
```

## 验证挂载目录是否更改

```bash
docker info
Docker Root Dir: /docker

```

```bash
docker ps -a
```

说明：这时候如果docker ps -a是不会有任何输出的。

## 复制文件到新的挂载目录

```bash
cp -arp /var/lib/docker/* /docker/
```

说明：提示是否覆盖文件，选择是。

## 重启docker

```bash
systemctl restart docker
```

## 验证旧容器是否正常

```bash
docker ps -a
CONTAINER ID        IMAGE                            COMMAND             CREATED             STATUS              PORTS                                              NAMES
11ea907c531e        jumpserver/jms_guacamole:1.5.3   "/init"             12 months ago       Up 4 minutes        127.0.0.1:8081->8080/tcp                           jms_guacamole
c9b5a730f6ec        jumpserver/jms_koko:1.5.3        "./entrypoint.sh"   12 months ago       Up 4 minutes        0.0.0.0:2222->2222/tcp, 127.0.0.1:5000->5000/tcp   jms_koko
```
