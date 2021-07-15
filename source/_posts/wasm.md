---
title: wasm项目
date: 2021-07-12 13:31:12
tags:
- istio
---


# wasm项目

## envoy为什么使用WASM filter

#### 通过 WASM filter的实现，我们可以得到：

* 敏捷性 - 过滤器可以动态加载到正在运行的 Envoy 进程中，而无需停止或重新编译。

* 可维护性 - 我们不必更改 Envoy 的代码库来扩展其功能。

* 多样性 - 流行的编程语言如 C/C++ 和 Rust 可以编译成 WASM，因此开发人员可以使用他们选择的编程语言来实现过滤器。

* 可靠性和隔离 - 过滤器部署到 VM（沙箱）中，因此与托管 Envoy 进程本身隔离（例如，当 WASM 过滤器崩溃时，它不会影响 Envoy 进程）。

* 安全性 - 由于过滤器通过定义良好的 API 与主机（Envoy 代理）通信，因此它们可以访问并且只能修改有限数量的连接或请求属性。

#### 它还具有一些需要考虑的缺点：

* 性能比原生 C++ 快约 70%。
* 由于需要启动一个或多个 WASM 虚拟机，因此内存使用量更高。

### envoy 代理 WASM SDK

Envoy Proxy 在基于堆栈的虚拟机中运行 WASM 过滤器，因此过滤器的内存与主机环境隔离。嵌入主机（Envoy Proxy）和 WASM 过滤器之间的所有交互都是通过 Envoy Proxy WASM SDK 提供的函数和回调实现的。WASM SDK 具有多种编程语言的实现，例如：

* C++
* rust
* AssemblyScript
* Go

在这篇文章中，我们将讨论如何使用Go Envoy Proxy WASM SDK为 Envoy 编写 WASM 过滤器。我们不打算详细讨论 Envoy Proxy WASM SDK 的 API，因为它超出了本文的范围。但是，我们将涉及掌握为 Envoy 编写 WASM 过滤器的基础知识所必需的一些内容。
我们的过滤器实现必须派生自以下两个类：
当加载 WASM 插件（包含过滤器的 WASM 二进制文件）时，会创建一个根上下文。根上下文与 VM 实例具有相同的生命周期，它执行我们的过滤器并用于：

```go

type rootContext struct {
	// You'd better embed the default root context
	// so that you don't need to reimplement all the methods by yourself.
	proxywasm.DefaultRootContext
}

type httpHeaders struct {
	// we must embed the default context so that you need not to reimplement all the methods by yourself
	proxywasm.DefaultHttpContext
	contextID uint32
}
```

1. 初始化wasm项目

```bash
➜  cmd git:(istio-1.9.4-dev) ✗ ./cmd init demo
 buildVersion = unknown, buildGitRevision = unknown, buildStatus = unknown, buildTag  = unknown, buildHub = unknown
Use the arrow keys to navigate: ↓ ↑ → ← 
? What language do you wish to use for the filter: 
  ▸ cpp
    rust
    assemblyscript
    tinygo

```

项目结构如下:

```tree
demo
|-- go.mod
|-- main.go
|-- runtime-config.json
```

2. 我们在代码中加上我们所需的代码 例如:
   在http的头中加上一个key="hello"  value="world"
```go
// Override DefaultHttpContext.
func (ctx *httpHeaders) OnHttpResponseHeaders(numHeaders int, endOfStream bool) types.Action {
	if err := proxywasm.SetHttpResponseHeader("hello", "world"); err != nil {
		proxywasm.LogCriticalf("failed to set response header: %v", err)
	}
	return types.ActionContinue
}
```

3. 编译demo项目

使用go语言构建wasm的时候需要安装tinygo

macos

```bash
brew install tinygo
```

安装完成后，在当前项目的根目录执行:

```bash
tinygo build -o filter.wasm -target=wasi -wasm-abi=generic .
```

执行当前命令后会生成 `filter.wasm`

4. 将wasm的包scp 到某个sidecar 容器中, 例如:

```bash
kubectl get po -n demo

NAME                              READY   STATUS    RESTARTS   AGE
details-v1-5588477696-2sw7b       2/2     Running   0          8d
productpage-v1-5bd6875444-j75dp   2/2     Running   0          8d
ratings-v1-c9d5c65fc-l65mq        2/2     Running   0          8d
reviews-v2-c789c7bdc-tsg7q        2/2     Running   0          8d
reviews-v3-78944b866f-96nbw       2/2     Running   0          8d

kubectl cp filter.wasm -n demo productpage-v1-5bd6875444-j75dp:/var/local/filter.wasm

```

我们查看一下容器中是否包含`filter.wasm`
```bash
➜  wasm git:(main) ✗ k exec -it productpage-v1-5bd6875444-j75dp ls /var/local
Defaulting container name to productpage.
Use 'kubectl describe pod/productpage-v1-5bd6875444-j75dp -n demo' to see all of the containers in this pod.
filter.wasm
```

可以看到/var/local 中包含`filter.wasm`

5. 创建envoyfilter

```yaml
kubectl apply -f-<<EOF
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: productpage-v1-examplefilter
  namespace: demo
spec:
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      proxy:
        proxyVersion: '^1\.8.*'
      listener:
        portNumber: 8080
        filterChain:
          filter:
            name: envoy.http_connection_manager
            subFilter:
              name: envoy.router
    patch:
      operation: INSERT_BEFORE
      value:
        config:
          config:
            name: productpage-demo
            rootId: my_root_id
            vmConfig:
              code:
                local:
                  filename: /var/local/filter.wasm
              runtime: envoy.wasm.runtime.v8
              vmId: filter
              allow_precompiled: true
        name: envoy.filters.http.wasm
  workloadSelector:
    labels:
      app: productpage
      version: v1
EOF
```

1. 向productpage服务上的 HTTP 端口 8080 发送一些流量：

```bash
kubectl run curl --image=xxx --restart=Never -it --rm sh

~ # curl -L -v http://productpage.demo:9080
```

在响应中，我们希望看到过滤器的标头添加到响应标头中：

```yaml
    * About to connect() to frontpage.backyards-demo port 8080 (#0)
    *   Trying 10.10.178.38...
    * Adding handle: conn: 0x10eadbd8
    * Adding handle: send: 0
    * Adding handle: recv: 0
    * Curl_addHandleToPipeline: length: 1
    * - Conn 0 (0x10eadbd8) send_pipe: 1, recv_pipe: 0
    * Connected to frontpage.backyards-demo (10.10.178.38) port 8080 (#0)
    > GET / HTTP/1.1
    > User-Agent: curl/7.30.0
    > Host: frontpage.backyards-demo:8080
    > Accept: */*
    >
    < HTTP/1.1 200 OK
    < content-type: text/plain
    < date: Thu, 16 Apr 2020 16:32:20 GMT
    < content-length: 9
    < x-envoy-upstream-service-time: 10
    < resp-header-demo: added by our filter
    < x-envoy-peer-metadata: CjYKDElOU1RBTkNFX0lQUxImGiQxMC4yMC4xLjU3LGZlODA6OmQwNDM6NDdmZjpmZWYwOmVkMjkK2QEKBkxBQkVMUxLOASrLAQoSCgNhcHASCxoJZnJvbnRwYWdlCiEKEXBvZC10ZW1wbGF0ZS1oYXNoEgwaCjU3OGM2NTU0ZDQKJAoZc2VjdXJpdHkuaXN0aW8uaW8vdGxzTW9k
    ZRIHGgVpc3RpbwouCh9zZXJ2aWNlLmlzdGlvLmlvL2Nhbm9uaWNhbC1uYW1lEgsaCWZyb250cGFnZQorCiNzZXJ2aWNlLmlzdGlvLmlvL2Nhbm9uaWNhbC1yZXZpc2lvbhIEGgJ2MQoPCgd2ZXJzaW9uEgQaAnYxChoKB01FU0hfSUQSDxoNY2x1c3Rlci5sb2NhbAonCgROQU1FEh8aHWZyb250cGFnZS12MS01N
    zhjNjU1NGQ0LWxidnFrCh0KCU5BTUVTUEFDRRIQGg5iYWNreWFyZHMtZGVtbwpXCgVPV05FUhJOGkxrdWJlcm5ldGVzOi8vYXBpcy9hcHBzL3YxL25hbWVzcGFjZXMvYmFja3lhcmRzLWRlbW8vZGVwbG95bWVudHMvZnJvbnRwYWdlLXYxCi8KEVBMQVRGT1JNX01FVEFEQVRBEhoqGAoWCgpjbHVzdGVyX2lkEg
    gaBm1hc3RlcgocCg9TRVJWSUNFX0FDQ09VTlQSCRoHZGVmYXVsdAofCg1XT1JLTE9BRF9OQU1FEg4aDGZyb250cGFnZS12MQ==
    < x-envoy-peer-metadata-id: sidecar~10.20.1.57~frontpage-v1-578c6554d4-lbvqk.backyards-demo~backyards-demo.svc.cluster.local
    < x-by-metadata: CjYKDElOU1RBTkNFX0lQUxImGiQxMC4yMC4xLjU3LGZlODA6OmQwNDM6NDdmZjpmZWYwOmVkMjkK2QEKBkxBQkVMUxLOASrLAQoSCgNhcHASCxoJZnJvbnRwYWdlCiEKEXBvZC10ZW1wbGF0ZS1oYXNoEgwaCjU3OGM2NTU0ZDQKJAoZc2VjdXJpdHkuaXN0aW8uaW8vdGxzTW9kZRIHGgVp
    c3RpbwouCh9zZXJ2aWNlLmlzdGlvLmlvL2Nhbm9uaWNhbC1uYW1lEgsaCWZyb250cGFnZQorCiNzZXJ2aWNlLmlzdGlvLmlvL2Nhbm9uaWNhbC1yZXZpc2lvbhIEGgJ2MQoPCgd2ZXJzaW9uEgQaAnYxChoKB01FU0hfSUQSDxoNY2x1c3Rlci5sb2NhbAonCgROQU1FEh8aHWZyb250cGFnZS12MS01NzhjNjU1N
    GQ0LWxidnFrCh0KCU5BTUVTUEFDRRIQGg5iYWNreWFyZHMtZGVtbwpXCgVPV05FUhJOGkxrdWJlcm5ldGVzOi8vYXBpcy9hcHBzL3YxL25hbWVzcGFjZXMvYmFja3lhcmRzLWRlbW8vZGVwbG95bWVudHMvZnJvbnRwYWdlLXYxCi8KEVBMQVRGT1JNX01FVEFEQVRBEhoqGAoWCgpjbHVzdGVyX2lkEggaBm1hc3
    RlcgocCg9TRVJWSUNFX0FDQ09VTlQSCRoHZGVmYXVsdAofCg1XT1JLTE9BRF9OQU1FEg4aDGZyb250cGFnZS12MQ==
    * Server istio-envoy is not blacklisted
    < server: istio-envoy
    < x-envoy-decorator-operation: frontpage.backyards-demo.svc.cluster.local:8080/*
    <
    * Connection #0 to host frontpage.backyards-demo left intact
    frontpage
    
```
