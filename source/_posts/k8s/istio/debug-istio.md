---
title: 如何在本地调试istio
date: 2021-03-22 13:40:42
tags:
---

本文档主要是帮助我们如何在本地使用调试istio

因为我们使用的istio都是在1.8.4上面做的,所以我在下面的讲解的版本也是在istio release-1.8.4版本上面进行

istio 核心模块有两个

* pilot-discovery: 这个模块就是我们的istiod istio/pilot/pilot-discovery 目录下
* pilot-agent: 这个模块 就是proxy istio/pilot/pilot-discovery 目录下

![img.png](img.png)

如果我们使用kind 则需要 --config trustworthy-jwt.yaml

```yaml
apiVersion: kind.x-k8s.io/v1alpha4
kind: Cluster
kubeadmConfigPatches:
  - |
    apiVersion: kubeadm.k8s.io/v1beta2
    kind: ClusterConfiguration
    metadata:
      name: config
    etcd:
      local:
        # Run etcd in a tmpfs (in RAM) for performance improvements
        dataDir: /tmp/kind-cluster-etcd
    apiServer:
      extraArgs:
        "service-account-issuer": "kubernetes.default.svc"
        "service-account-signing-key-file": "/etc/kubernetes/pki/sa.key"
containerdConfigPatches:
  - |-
    [plugins."io.containerd.grpc.v1.cri".registry.mirrors."localhost:5000"]
      endpoint = ["http://kind-registry:5000"]
```

### 运行kind

```bash
$ kind create cluster --image  docker.io/kindest/node:v1.17.5 --config trustworthy-jwt.yaml  --name kind-2
```

### 本地代理`pilot-discovery`

```bash
# 安装istio
$ istioctl install
```

等待istio 安装完成以后我们将 istiod的流量转到本地，主要目的是为了更改endpoint 

使用外部 istiod

```bash
# 设置外部 IP
$ export ip=192.168.110.206
$ kubectl -n istio-system delete svc istiod
$ kubectl -n istio-system delete endpoints istiod

$ cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: istiod
  namespace: istio-system
spec:
  ports:
  - name: grpc-xds
    port: 15010
  - name: https-dns
    port: 15012
  - name: https-webhook
    port: 443
    targetPort: 15017
  - name: http-monitoring
    port: 15014
---
apiVersion: v1
kind: Endpoints
metadata:
  name: istiod
  namespace: istio-system
subsets:
- addresses:
  - ip: ${ip}
  ports:
  - name: https-dns
    port: 15012
    protocol: TCP
  - name: grpc-xds
    port: 15010
    protocol: TCP
  - name: https-webhook
    port: 15017
    protocol: TCP
  - name: http-monitoring
    port: 15014
    protocol: TCP

EOF
```

还原istio 的svc地址 使svc指向 集群内部 istiod.istio-system pod

```bash
$ kubectl -n istio-system delete svc istiod
$ kubectl -n istio-system delete endpoints istiod

cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  annotations:
    kubectl.kubernetes.io/last-applied-configuration: |
      {"apiVersion":"v1","kind":"Service","metadata":{"annotations":{},"labels":{"app":"istiod","install.operator.istio.io/owning-resource":"unknown","install.operator.istio.io/owning-resource-namespace":"istio-system","istio":"pilot","istio.io/rev":"default","operator.istio.io/component":"Pilot","operator.istio.io/managed":"Reconcile","operator.istio.io/version":"1.8.4","release":"istio"},"name":"istiod","namespace":"istio-system"},"spec":{"ports":[{"name":"grpc-xds","port":15010,"protocol":"TCP"},{"name":"https-dns","port":15012,"protocol":"TCP"},{"name":"https-webhook","port":443,"protocol":"TCP","targetPort":15017},{"name":"http-monitoring","port":15014,"protocol":"TCP"}],"selector":{"app":"istiod","istio":"pilot"}}}
  creationTimestamp: "2021-03-22T07:08:32Z"
  labels:
    app: istiod
    install.operator.istio.io/owning-resource: unknown
    install.operator.istio.io/owning-resource-namespace: istio-system
    istio: pilot
    istio.io/rev: default
    operator.istio.io/component: Pilot
    operator.istio.io/managed: Reconcile
    operator.istio.io/version: 1.8.4
    release: istio
  name: istiod
  namespace: istio-system
  resourceVersion: "826"
  selfLink: /api/v1/namespaces/istio-system/services/istiod
  uid: 44ee44a6-9003-4f8d-9196-49e47b6667c1
spec:
  clusterIP: 10.96.3.250
  ports:
  - name: grpc-xds
    port: 15010
    protocol: TCP
    targetPort: 15010
  - name: https-dns
    port: 15012
    protocol: TCP
    targetPort: 15012
  - name: https-webhook
    port: 443
    protocol: TCP
    targetPort: 15017
  - name: http-monitoring
    port: 15014
    protocol: TCP
    targetPort: 15014
  selector:
    app: istiod
    istio: pilot
  sessionAffinity: None
  type: ClusterIP
status:
  loadBalancer: {}
EOF  
```

