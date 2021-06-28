---
title: 集群内部负载均衡 lb
date: 2021-04-07 22:50:01
tags:
- kubernetes
---

# 集群内部负载均衡 lb

k8s的LoadBalancer类型的Service依赖云服务商的Load Balancer, 如阿里云的slb。


当我们把k8s部署在私有云时，需要简单的LoadBalancer来验证工作，开源的metallb就是一个不错的选择。

MetalLB支持2种 一种是`Layer2` 、`BGP`

## MetalLB 安装

```bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.9.3/manifests/namespace.yaml;
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey="$(openssl rand -base64 128)";
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.9.3/manifests/metallb.yaml;
```

## 前提条件

MetalLB需要以下功能才能发挥作用：

* 一个 Kubernetes 运行Kubernetes 1.13.0或更高版本的集群，尚不具有网络负载平衡功能。
* 一个 集群网络配置 可以与MetalLB共存。
* 一些用于MetalLB的IPv4地址。
* 使用BGP工作模式时，您将需要一台或多台能够讲话的路由器 BGP协议。
* 节点之间必须允许端口7946（TCP＆UDP）上的流量，具体取决于 会员列表。


GBP 配置

```bash
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    peers:
    - peer-address: 10.0.0.1
      peer-asn: 64501
      my-asn: 64500
    address-pools:
    - name: default
      protocol: bgp
      addresses:
      - 192.168.10.0/24
```

### 创建configmap

```bash
kubectl apply -f configmap.yaml
```