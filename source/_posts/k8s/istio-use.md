---
title: istio 流量管理使用说明
date: 2021-01-21
tags:
- istio
---

istio 流量管理使用说明

### 前提条件

在开始之前,请检查以下先决条件：

1. 检查kubernetes 集群pod和 <font face="黑体" color=green size=3>服务</font>
2. 使用1.6 以上版本的istio,并检查 pod和服务
3. 设定 必要的平台设置

### 部署测试服务 bookinfo

我们就使用istio 官方使用的测试服务`BookInfo`

Bookinfo应用程序分为四个单独的微服务：

productpage。该productpage微服务调用details和reviews微服务来填充页面。
details。该details微服务包含图书信息。
reviews。该reviews微服务包含了书评。它还称为ratings微服务。
ratings。该ratings微服务包含预定伴随书评排名信息。
reviews微服务有3个版本：

版本v1不会调用该ratings服务。
版本v2调用该ratings服务,并将每个等级显示为1到5个黑星。
版本v3调用该ratings服务,并将每个等级显示为1到5个红色星号。

访问关系如下图

![bookInfo](/uploads/istio/images/m_88841685b364bda18fb333e9acb97366_r.png)

首先部署bookinfo服务之前我们需要给 bookinfo 注入
sidecar,若给namespace打 `istio-injection=enabled`的labels 则istio会给当前namespace下的所有pod自动注入sidecar

假设我们将bookinfo 部署到demo的这个namespace中

```bash

$ kubectl label namespace demo istio-injection=enabled
```

查看labels是否设置成功

```bash
$ kubectl get namespace demo --show-labels
```

然后你就看到一下的效果 如果最后一个字段上面显示`istio-injection=enabled`则代表labels 设置成功

```text
NAME   STATUS   AGE   LABELS
demo   Active   24s   istio-injection=enabled
```

部署 BookInfo

```bash
$ kubectl apply -f istio/bookinfo.yml
```

查看 服务是否正常部署且 成功注入sidecar

```bash
$ kubectl get po -n demo
```

确认 所有的service 和pod 都Running 

```bash
$ kubectl get service -n demo 

NAME          TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)    AGE
details       ClusterIP   10.0.0.31    <none>        9080/TCP   6m
kubernetes    ClusterIP   10.0.0.1     <none>        443/TCP    7d
productpage   ClusterIP   10.0.0.120   <none>        9080/TCP   6m
ratings       ClusterIP   10.0.0.15    <none>        9080/TCP   6m
reviews       ClusterIP   10.0.0.170   <none>        9080/TCP   6m


$ kubectl get pods -n demo

NAME                             READY     STATUS    RESTARTS   AGE
details-v1-1520924117-48z17      2/2       Running   0          6m
productpage-v1-560495357-jk1lz   2/2       Running   0          6m
ratings-v1-734492171-rnr5l       2/2       Running   0          6m
reviews-v1-874083890-f0qf0       2/2       Running   0          6m
reviews-v2-1343845940-b34q5      2/2       Running   0          6m
reviews-v3-1813607990-8ch52      2/2       Running   0          6m
``` 

如果所有pod ready 显示 2/2 则 服务正确注入sidecar

### 流量管理


##### 配置服务访问

如何将请求动态路由到微服务的多个版本。

istio Bookinfo示例包含四个单独的微服务,每个微服务具有多个版本。微服务之一的三种不同版本reviews已经部署并同时运行。为了说明此问题,请/productpage在浏览器中访问Bookinfo应用,然后刷新几次。您会注意到,有时书评输出中包含星级,有时则不。这是因为如果没有明确的默认服务版本可路由,Istio将以循环方式将请求路由到所有可用版本。

配置方式如下所示

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: bookinfo-gateway
  namespace: demo
spec:
  selector:
    istio: ingressgateway # istio-system 下面的istio-ingressgateway 的labels
  servers:
  - hosts:
    - '*' # 指定访问域名
    port:
      name: http 
      number: 80 # 访问端口
      protocol: HTTP # 访问协议名称

---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: bookinfo
  namespace: demo
spec:
  gateways:
  - bookinfo-gateway # 网关gateway的名称
  hosts:
  - '*' # 访问的svc 的名称
  http:
  - match:
    - uri:
        exact: /productpage # 精确匹配 路径
    - uri:
        prefix: /static # 前缀匹配路径
    - uri:
        exact: /login 
    - uri:
        exact: /logout
    - uri:
        prefix: /api/v1/products
    route:
    - destination:
        host: productpage # 目的服务 svc地址
        port:
          number: 9080 # 目的端口


```

以上配置的名词解释

网关配置 gateway 必要字段的解释

|  字段名称 | 类型  | 描述  | 是否必须   |
| ------------ | ------------ | ------------ | ------------ |
|  selector  | map<string, string>  | 一个或多个标签,指示应在其上应用此网关配置的一组特定的Pod / VM。 默认情况下,基于标签选择器在所有名称空间中搜索工作负载。 这意味着名称空间“ foo”中的网关资源可以基于标签选择名称空间“ bar”中的pod。 可以通过istiod中的`PILOTSCOPEGATEWAYTONAMESPACE`环境变量来控制此行为。 如果将此变量设置为true,则标签搜索的范围将限于存在资源的配置名称空间。 换句话说,网关资源必须与网关工作负载实例位于相同的名称空间中。 如果选择器为零,则网关将应用于所有工作负载。  |  yes |
|  hosts | []string  | 此网关公开的一台或多台主机。 尽管通常适用于HTTP服务,但也可以将其用于使用TLS和SNI的TCP服务。 主机被指定为带有可选名称空间/前缀的dnsName。 dnsName应该使用FQDN格式指定,可以选择在最左侧的组件中包含通配符（例如prod / *.example.com）。 将dnsName设置为*,以从指定的名称空间（例如prod / *）中选择所有VirtualService主机。可以将名称空间设置为*或。,分别代表任意名称空间或当前名称空间。 例如,* / foo.example.com从任何可用的名称空间中选择服务,而./foo.example.com仅从Sidecar的名称空间中选择服务。 如果未指定名称空间/,则默认值为* /,即从任何名称空间中选择服务。 还将使用所选名称空间中的任何关联的DestinationRule。虚拟服务必须绑定到网关,并且必须具有一个或多个与服务器中指定的主机匹配的主机。 匹配可以是与服务器主机的完全匹配或后缀匹配。 例如,如果服务器的主机指定* .example.com,则具有主机dev.example.com或prod.example.com的VirtualService将匹配。 但是,带有主机example.com或newexample.com的VirtualService将不匹配。注意：仅可以引用导出到网关名称空间的虚拟服务（例如,exportTo值为*）。 私有配置（例如,exportTo设置为）将不可用。 有关详细信息,请参考VirtualService,DestinationRule和ServiceEntry配置中的exportTo设置。 | yes  |
|port |port |代理应在其上侦听传入连接的端口和协议| yes|



获取效果如下

```bash

➜  ~ kubectl get gw -n demo
NAME               AGE
bookinfo-gateway   11m
➜  ~ kubectl get vs -n demo
NAME                         GATEWAYS             HOSTS                                  AGE
bookinfo                     [bookinfo-gateway]   [*]                                    11m
```

##### VirtualService

定义了一组寻址主机时要应用的流量路由规则。每个路由规则为特定协议的流量定义匹配条件。如果流量匹配,则将其发送到注册表中定义的命名目标服务（或其子集/版本）。

以下为VirtualService 字段的名词解释


|  字段名称 | 类型  | 描述  | 是否必须   |
| ------------ | ------------ | ------------ | ------------ |
| hosts   | string[]  | hosts字段适用于HTTP和TCP服务。网格内的服务（即在服务注册表中找到的服务）必须始终使用其字母数字名称进行引用。IP地址仅允许通过网关定义的服务使用。  |  no |
| gateways   | string[]  | 应应用这些路由的网关和sidecar的名称。其他名称空间中的网关可以由<gateway namespace>/<gateway name>;引用 。指定没有名称空间限定符的网关与指定VirtualService的名称空间相同  |  no |
|  subset |  string | 服务中子集的名称。仅适用于网格内的服务。该子集必须在相应的DestinationRule中定义。  | node  |
|  port  | PortSelector  | 指定要寻址的主机上的端口。如果服务仅公开单个端口,则不需要显式选择端口。|  no |
| http   |  HTTPRoute[] | HTTP流量的路由规则的有序列表。HTTP路由将应用于名为“ http- ” /“ http2- ” /“ grpc- *”的平台服务端口,协议为HTTP / HTTP2 / GRPC / TLS终止的HTTPS的网关端口以及使用HTTP / HTTP2 /的服务入口端口GRPC协议。使用匹配传入请求的第一条规则。|  no |
| tls   | TLSRoute[]  | 未终止的TLS和HTTPS流量的路由规则的有序列表。路由通常使用ClientHello消息显示的SNI值执行。TLS路由将应用于使用HTTPS / TLS协议（即采用“直通” TLS模式）的名为“ https- ”,“ tls- ”的平台服务端口,未终止的网关端口以及使用HTTPS / TLS协议的服务入口端口。使用匹配传入请求的第一条规则。注意：没有关联虚拟服务的流量“ https- ”或“ tls- ”端口将被视为不透明的TCP流量。  | no  |
| tcp  | TCPRoute[]  | 不透明TCP流量的路由规则的有序列表。TCP路由将应用于不是HTTP或TLS端口的任何端口。使用匹配传入请求的第一条规则。  | no  |


##### HTTP路由

描述用于路由HTTP / 1.1,HTTP2和gRPC通信的匹配条件和操作。有关用法示例,请参见VirtualService。

|  字段名称 | 类型  | 描述  | 是否必须   |
| ------------ | ------------ | ------------ | ------------ |
| name  | string   |  分配给路由以进行调试的名称。路由名称将与匹配名称串联在一起,并将被记录在访问日志中,以查找与此路由/匹配匹配的请求。 | no  |
| match   | HTTPMatchRequest[]  | 匹配要激活的规则要满足的条件。单个匹配块内的所有条件都具有AND语义,而匹配块列表具有OR语义。如果任何一个匹配块成功,则匹配该规则。  |  no |
| route   |  HTTPRouteDestination[] |  HTTP规则可以重定向或转发（默认）流量。转发目标可以是服务的多个版本之一（请参阅文档开头的词汇表）。与服务版本关联的权重决定了它接收的流量比例。 | no  |
| redirect   | HTTPRedirect  |  HTTP规则可以重定向或转发（默认）流量。如果在规则中指定了流量通过选项,则将忽略路由/重定向。重定向原语可用于将HTTP 301重定向发送到其他URI或Authority。 | no  |
|  rewrite  | HTTPRewrite  |  重写HTTP URI和Authority标头。重写不能与重定向原语一起使用。重写将在转发之前执行。 | no |
| timeout  | 	Duration|HTTP请求超时,默认为禁用。  |  no|
|retries|HTTPRetry | 重试HTTP请求的策略。| no |
|headers|Headers|标头操作规则|no|
|fault|	HTTPFaultInjection|故障注入策略,适用于客户端的HTTP通信。请注意,如果在客户端启用了错误,则不会启用超时或重试。|没有|
|mirror|Destination|除了将请求转发到预期目标之外,还将HTTP流量镜像到另一个目标。镜像流量是基于尽力而为的,在这种情况下,边车/网关在从原始目的地返回响应之前不会等待镜像集群响应。将为镜像目标生成统计信息。|no|

http 和tcp 、tls 基本上类似 就不重复说明了

访问页面即可看到页面效果如下:

![](/uploads/istio/images/m_f048c09d102e39ddf43a9d3e0ae81d47_r.png)

红色框的地方就是不同版本的 `reviews` 服务

##### 请求超时

要测试Bookinfo应用程序微服务的弹性,请为用户reviews:v2和的ratings微服务之间注入7s的延迟jason。此测试将发现故意引入Bookinfo应用程序的错误。

```yaml
$ kubectl get virtualservice ratings -o yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: bookinfo-fault-injection
  namespace: demo
spec:
  hosts:
  - ratings
  http:
  - fault:
      delay:
        fixedDelay: 7s
        percentage:
          value: 100
    match:
    - headers:
        end-user:
          exact: jason
    route:
    - destination:
        host: ratings
        subset: v1
  - route:
    - destination:
        host: ratings
        subset: v1
```

产生的效果如下所示：

重新加载`productpage`网页。您将看到该页面实际上在大约6秒钟内加载完成。

<font face="黑体" color=red size=3>意义:</font>Istio启用协议特定的故障注入到网络中,而不是杀死pod,延迟或在TCP层破坏数据包。我们的理由是,无论网络级别的故障如何,应用层观察到的故障都是一样的,并且可以在应用层注入更有意义的故障（例如,HTTP错误代码）,以锻炼应用的弹性。

运维人员可以为符合特定条件的请求配置故障。运维人员可以进一步限制应该遭受故障的请求的百分比。可以注入两种类型的故障：延迟和中止。延迟是计时故障,模拟增加的网络延迟或过载的上游服务。中止是模拟上游服务的崩溃故障。中止通常以HTTP错误代码或TCP连接失败的形式表现。



##### 流量分配

一个常见的用例是将流量逐渐从一种微服务版本迁移到另一种。在Istio中,您可以通过配置一系列规则以将一定百分比的流量路由到一项服务或另一项服务来实现此目标。在此任务中,您将发送流量的10％reviews:v1和90％ reviews:v3。然后,您将100％的流量发送到来完成迁移reviews:v3。

现在来创建我的配置吧

```bash
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: bookinfo-traffic-injection
  namespace: demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1 # 需要和DestinationRule 资源的名字对应起来
      weight: 10
    - destination:
        host: reviews
        subset: v3
      weight: 90
---

apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: bookinfo-traffic-injection
  namespace: demo
spec:
  host: reviews
  subsets:
  - name: v1 # subset指向的名称
    labels:
      version: v1 # 需要路由到pod labels中的version版本对应的值
  - name: v2
    labels:
      version: v2
  - name: v3
    labels:
      version: v3
```

创建以上配置

```yaml
$ kubectl apply -f bookinfo-traffic-injection.yaml
```


将istio-system下面的kiali的svc地址改成`nodeport` 访问即可, 默认登陆的用户名密码是 admin/admin

![](/uploads/istio/images/m_2d6a4a0feac0159ee65ac3aee53ecaaf_r.png)

以上是我们还没有配置分配流量时的流量视图,我们可以看到`reviews`这个服务的三个不同版本的pod都是平均分配的 大约所占比例是33%左右。

现在我们配置了流量分配再看看是什么样子的。

我们会发现红色星的次数明显要多余没有星的次数多很多

我们现在可以借助kiali来看看我们请求的流量视图

![](/uploads/istio/images/m_010ffca5e15ec7eb30397a2e97db1268_r.png)

可以看到`reviews`不同版本的服务所占比例分别是

`v1 --->14.3% ,v2 ---> 0% ,  v3 ---> 85.7%`

现在我们istio的流量分配的设置已经成功了,那么istio的流量分配的意义在哪里,有什么作用呢?

<font face="黑体" color=red size=3>意义:</font>您可以通过 istio 指定特定服务按照你的设定指定转到金丝雀版本,而不必考虑金丝雀部署的大小,或根据请求的内容将流量发送到特定版本。

##### 给服务注入故障

##### 熔断

##### 流量镜像

##### 问题定位

在istio中有一个很麻烦的问题 就是配置出错以后很难定位是什么配置错误导致访问出错的问题,所以我们需要专门的工具来定位是什么导致的配置问题。


#### 安全

##### 证书管理

##### 认证方式

### 参考文档