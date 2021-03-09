---
title: kiali 源码解析
tags: kiali
date: 2021-03-09 18:36:28
---


kiali 源码解析

前言: 

Kiali是用于基于Istio的服务网格的管理控制台。它提供仪表板，可观察性，并允许您使用强大的配置和验证功能来操作网格。它通过推断流量拓扑来显示服务网格的结构，并显示网格的运行状况。Kiali提供了详细的指标，强大的验证，Grafana访问以及与Jaeger进行分布式跟踪的强大集成。


以下就是kiali的流量试图 界面

![kiali](https://kiali.io/images/documentation/features/graph-health-v1.22.0.png)

Kiali是用于基于Istio的服务网格的管理控制台 所以得使用istio来安装kiali插件

如何istio 来安装kiali

```bash
$ istioctl manifest apply --set values.kiali.enabled=true
```

kiali 的默认 用户名与密码是 admin/admin

## kiali 流量试图

### kiali route 路由

routing/router

```go
// 根路由 指向 静态页面
if webRoot != "/" {
		rootRouter.HandleFunc(webRoot, func(w http.ResponseWriter, r *http.Request) {
			http.Redirect(w, r, webRootWithSlash, http.StatusFound)
		})

		// help the user out - if a request comes in for "/", redirect to our true webroot
		rootRouter.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
			http.Redirect(w, r, webRootWithSlash, http.StatusFound)
		})

		appRouter = rootRouter.PathPrefix(conf.Server.WebRoot).Subrouter()
	}
	appRouter = appRouter.StrictSlash(true)

// 路由指向代理的function 
	r.Routes = []Route{
		// swagger:route GET /healthz kiali healthz
		// ---
		// Endpoint to get the health of Kiali
		//
		//     Produces:
		//     - application/json
		//
		//     Schemes: http, https
		// responses:
		//		500: internalError
		//		200
		{
			"Healthz",
			"GET",
			"/healthz",
			handlers.Healthz,
			false,
		},
		// swagger:route GET / kiali root
		// ---
		// Endpoint to get the status of Kiali
		//
		//     Produces:
		//     - application/json
		//
		//     Schemes: http, https
		// responses:
		//      500: internalError
		//      200: statusInfo
		{
			"Root",
			"GET",
			"/api",
			handlers.Root,
			false,
		}
}
```


kiali里面最复杂的就是流量视图了 现在我们看看kiali是怎么做的

/graph/api/api

```go

func GraphNamespaces(business *business.Layer, o graph.Options) (code int, config interface{}) {
	// time how long it takes to generate this graph
	promtimer := internalmetrics.GetGraphGenerationTimePrometheusTimer(o.GetGraphKind(), o.TelemetryOptions.GraphType, o.InjectServiceNodes)
	defer promtimer.ObserveDuration()

	switch o.TelemetryVendor {
	case graph.VendorIstio:
		prom, err := prometheus.NewClientNoAuth(business.PromAddress)
		graph.CheckError(err)
//获取config 蓝图信息
		code, config = graphNamespacesIstio(business, prom, o)
	default:
		graph.Error(fmt.Sprintf("TelemetryVendor [%s] not supported", o.TelemetryVendor))
	}

	// update metrics
	internalmetrics.SetGraphNodes(o.GetGraphKind(), o.TelemetryOptions.GraphType, o.InjectServiceNodes, 0)

	return code, config
}
```


构造 TrafficMap

graph/telemetry/istio/istio.go

```go

// BuildNamespacesTrafficMap is required by the graph/TelemtryVendor interface
func BuildNamespacesTrafficMap(o graph.TelemetryOptions, client *prometheus.Client, globalInfo *graph.AppenderGlobalInfo) graph.TrafficMap {
	log.Tracef("Build [%s] graph for [%v] namespaces [%s]", o.GraphType, len(o.Namespaces), o.Namespaces)

	setLabels()
	appenders := appender.ParseAppenders(o)
	trafficMap := graph.NewTrafficMap()

	for _, namespace := range o.Namespaces {
		log.Tracef("Build traffic map for namespace [%s]", namespace)
		//生成一个 namespaceTrafficMap
		namespaceTrafficMap := buildNamespaceTrafficMap(namespace.Name, o, client)
		namespaceInfo := graph.NewAppenderNamespaceInfo(namespace.Name)
		for _, a := range appenders {
			appenderTimer := internalmetrics.GetGraphAppenderTimePrometheusTimer(a.Name())
			a.AppendGraph(namespaceTrafficMap, globalInfo, namespaceInfo)
			appenderTimer.ObserveDuration()
		}
		// 将 namespaceTrafficMap merge ---->  trafficMap 中
		telemetry.MergeTrafficMaps(trafficMap, namespace.Name, namespaceTrafficMap)
	}

	// The appenders can add/remove/alter nodes. After the manipulations are complete
	// we can make some final adjustments:
	// - mark the outsiders (i.e. nodes not in the requested namespaces)
	// - mark the insider traffic generators (i.e. inside the namespace and only outgoing edges)
	telemetry.MarkOutsideOrInaccessible(trafficMap, o)
	telemetry.MarkTrafficGenerators(trafficMap)

	if graph.GraphTypeService == o.GraphType {
		trafficMap = telemetry.ReduceToServiceGraph(trafficMap)
	}

	return trafficMap
}
```

Appender 这个interface 主要负责获取和组装流量视图的节点信息和线的信息

graph/appender.go

```go
// Appender is implemented by any code offering to append a service graph with
// supplemental information.  On error the appender should panic and it will be
// handled as an error response.
type Appender interface {
	// AppendGraph performs the appender work on the provided traffic map. The map
	// may be initially empty. An appender is allowed to add or remove map entries.
	AppendGraph(trafficMap TrafficMap, globalInfo *AppenderGlobalInfo, namespaceInfo *AppenderNamespaceInfo)
	// Name returns a unique appender name and which is the name used to identify the appender (e.g in 'appenders' query param)
	Name() string
}

```

#### appender中有几种实现

* istio: 负责标记具有特殊Istio意义的节点
* deadNode: 负责从图中删除不需要的节点
* serviceEntry: ServiceEntryAppender负责标识在Istio中定义为serviceEntry的服务节点。
单个serviceEntry可以定义多个主机，
因此多个服务节点可以
映射到单个serviceEntry的不同主机。我们将这些称为“ se-service”节点
* responseTime: ResponseTimeAppender负责将responseTime信息添加到图形中
* securityPolicy: SecurityPolicyAppender负责向图表添加securityPolicy信息。
尽管以通用方式编写，但该附加程序当前仅报告international_tls安全性。
* sidecarsCheck: SidecarsCheckAppender标记其后备工作负载缺少至少一个Envoy sidecar的节点。请注意，
没有后备工作负载的节点未标记。
* unusedNode: 调用函数成功时，函数处理时间指标的持续时间值。
如果不成功，则递增失败计数器。
如果围棋函数不在一个类型上（即是一个全局函数），请为goType传入一个空字符串。
当该函数返回时，定时器立即开始计时。

appender中的实现必须得有先后顺序, service-entry --> deadNode --> responseTime ---> securityPolicy --->  unusedNode --> istio

经过appender之后将trafficMap merge 到 traffic map中

```go
trafficMap := graph.NewTrafficMap()

// 将 namespaceTrafficMap merge ---->  trafficMap 中

telemetry.MergeTrafficMaps(trafficMap, namespace.Name, namespaceTrafficMap)
```

