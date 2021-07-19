---
title: 在istio1.9中iptables链规则的意义
date: 2021-07-19 23:23:13
tags:
- kubernetes
- istio
---

## 在istio1.9中iptables链规则的意义

我们在k8s集群中安装1.9.4 版本的 istio 。

基于 istio1.9.4 版本，我们主要介绍istio在虚拟机中对iptables设置规则是什么样的？

在虚拟机中安装istio中, istio进程交给system进程保管，
进程中主要是运行了一个脚本，
我们可以看到istio dep 包的脚本如下:

```bash
$ cat /usr/local/bin/istio-start.sh

#!/bin/bash
#
# Copyright Istio Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################
#
# Script to configure and start the Istio sidecar.

set -e

# Match pilot/docker/Dockerfile.proxyv2
# istio 的版本
export ISTIO_META_ISTIO_VERSION="1.9.0"

set -a
# Load optional config variables
# 读取配置文件
ISTIO_SIDECAR_CONFIG=${ISTIO_SIDECAR_CONFIG:-/var/lib/istio/envoy/sidecar.env}
if [[ -r ${ISTIO_SIDECAR_CONFIG} ]]; then
  # shellcheck disable=SC1090
  . "$ISTIO_SIDECAR_CONFIG"
fi

# Load config variables ISTIO_SYSTEM_NAMESPACE, CONTROL_PLANE_AUTH_POLICY
ISTIO_CLUSTER_CONFIG=${ISTIO_CLUSTER_CONFIG:-/var/lib/istio/envoy/cluster.env}
if [[ -r ${ISTIO_CLUSTER_CONFIG} ]]; then
  # shellcheck disable=SC1090
  . "$ISTIO_CLUSTER_CONFIG"
fi
set +a

# Set defaults
ISTIO_BIN_BASE=${ISTIO_BIN_BASE:-/usr/local/bin}
ISTIO_LOG_DIR=${ISTIO_LOG_DIR:-/var/log/istio}
NS=${ISTIO_NAMESPACE:-default}
SVC=${ISTIO_SERVICE:-rawvm}
ISTIO_SYSTEM_NAMESPACE=${ISTIO_SYSTEM_NAMESPACE:-istio-system}

# The default matches the default istio.yaml - use sidecar.env to override this if you
# enable auth. This requires node-agent to be running.
# istiod 控制面的网关地址
ISTIO_PILOT_PORT=${ISTIO_PILOT_PORT:-15012}

# If set, override the default
CONTROL_PLANE_AUTH_POLICY=${ISTIO_CP_AUTH:-"MUTUAL_TLS"}

if [ -z "${ISTIO_SVC_IP:-}" ]; then
  ISTIO_SVC_IP=$(hostname --all-ip-addresses | cut -d ' ' -f 1)
fi

if [ -z "${POD_NAME:-}" ]; then
  POD_NAME=$(hostname -s)
fi

# Init option will only initialize iptables. set ISTIO_CUSTOM_IP_TABLES to true if you would like to ignore this step
# 初始化iptables 目的讲虚拟机上面的流量打到envoy中
if [ "${ISTIO_CUSTOM_IP_TABLES}" != "true" ] ; then
  # 如果是初始化容器 
    if [[ ${1-} == "init" || ${1-} == "-p" ]] ; then
      # clean the previous Istio iptables chains. This part is different from the init image mode,
      # where the init container runs in a fresh environment and there cannot be previous Istio chains
      
      
      # 在istio启动的时候应该清理掉所有的iptables的链
      "${ISTIO_BIN_BASE}/pilot-agent" istio-clean-iptables

      # Update iptables, based on current config. This is for backward compatibility with the init image mode.
      # The sidecar image can replace the k8s init image, to avoid downloading 2 different images.
      
      # 根据规则设置iptables的链
      
      "${ISTIO_BIN_BASE}/pilot-agent" istio-iptables "${@}"
      exit 0
    fi
    # 如果没有在运行的时候
    if [[ ${1-} != "run" ]] ; then
      # clean the previous Istio iptables chains. This part is different from the init image mode,
      # where the init container runs in a fresh environment and there cannot be previous Istio chains
      "${ISTIO_BIN_BASE}/pilot-agent" istio-clean-iptables

      # Update iptables, based on config file
      "${ISTIO_BIN_BASE}/pilot-agent" istio-iptables
    fi
fi

EXEC_USER=${EXEC_USER:-istio-proxy}
if [ "${ISTIO_INBOUND_INTERCEPTION_MODE}" = "TPROXY" ] ; then
  # In order to allow redirect inbound traffic using TPROXY, run envoy with the CAP_NET_ADMIN capability.
  # This allows configuring listeners with the "transparent" socket option set to true.
  EXEC_USER=root
fi

if [ -z "${PILOT_ADDRESS:-}" ]; then
  PILOT_ADDRESS=istiod.${ISTIO_SYSTEM_NAMESPACE}.svc:${ISTIO_PILOT_PORT}
fi

CA_ADDR=${CA_ADDR:-${PILOT_ADDRESS}}
PROV_CERT=${PROV_CERT-/etc/certs}
OUTPUT_CERTS=${OUTPUT_CERTS-/etc/certs}

export PROV_CERT
export OUTPUT_CERTS
export CA_ADDR

# If predefined ISTIO_AGENT_FLAGS is null, make it an empty string.
ISTIO_AGENT_FLAGS=${ISTIO_AGENT_FLAGS:-}
# Split ISTIO_AGENT_FLAGS by spaces.
IFS=' ' read -r -a ISTIO_AGENT_FLAGS_ARRAY <<< "$ISTIO_AGENT_FLAGS"

export PROXY_CONFIG=${PROXY_CONFIG:-"
serviceCluster: $SVC
controlPlaneAuthPolicy: ${CONTROL_PLANE_AUTH_POLICY}
discoveryAddress: ${PILOT_ADDRESS}
"}

if [ ${EXEC_USER} == "${USER:-}" ] ; then
  # if started as istio-proxy (or current user), do a normal start, without
  # redirecting stderr.
  INSTANCE_IP=${ISTIO_SVC_IP} POD_NAME=${POD_NAME} POD_NAMESPACE=${NS} "${ISTIO_BIN_BASE}/pilot-agent" proxy "${ISTIO_AGENT_FLAGS_ARRAY[@]}"
else

# Will run: ${ISTIO_BIN_BASE}/envoy -c $ENVOY_CFG --restart-epoch 0 --drain-time-s 2 --parent-shutdown-time-s 3 --service-cluster $SVC --service-node 'sidecar~${ISTIO_SVC_IP}~${POD_NAME}.${NS}.svc.cluster.local~${NS}.svc.cluster.local' $ISTIO_DEBUG >${ISTIO_LOG_DIR}/istio.log" istio-proxy
exec su -s /bin/bash -c "INSTANCE_IP=${ISTIO_SVC_IP} POD_NAME=${POD_NAME} POD_NAMESPACE=${NS} exec ${ISTIO_BIN_BASE}/pilot-agent proxy ${ISTIO_AGENT_FLAGS_ARRAY[*]} 2> ${ISTIO_LOG_DIR}/istio.err.log > ${ISTIO_LOG_DIR}/istio.log" ${EXEC_USER}
fi
```

### 运行的进程为

```bash
# INSTANCE_IP 当前虚拟机的ip
# 虚拟机中设置的ip
# 运行 pilot-agent proxy 当前进程
$ su -s /bin/bash -c INSTANCE_IP=10.10.13.113 POD_NAME=localhost POD_NAMESPACE=vm exec /usr/local/bin/pilot-agent proxy  2> /var/log/istio/istio.err.log > /var/log/istio/istio.log istio-proxy
```

我们来看看pilot-agent proxy 具体干了些什么吧？

我们来到istio/pilot/cmd/pilot-agent/main.go

```bash

			agent := envoy.NewAgent(envoyProxy, drainDuration)

			// Watcher is also kicking envoy start.
			watcher := envoy.NewWatcher(agent.Restart)
			go watcher.Run(ctx)

			// On SIGINT or SIGTERM, cancel the context, triggering a graceful shutdown
			go cmd.WaitSignalFunc(cancel)
            # 前面是数据组装我们就不过多的讲解, 最后一句agent 运行 envoy 
			return agent.Run(ctx)
			
		# 接下来我们看看实现
		
		
		
func (a *agent) Run(ctx context.Context) error {
	log.Info("Starting proxy agent")
	for {
		select {
		case status := <-a.statusCh:
			a.mutex.Lock()
			if status.err != nil {
				if status.err.Error() == errOutOfMemory {
					log.Warnf("Envoy may have been out of memory killed. Check memory usage and limits.")
				}
				log.Errorf("Epoch %d exited with error: %v", status.epoch, status.err)
			} else {
				log.Infof("Epoch %d exited normally", status.epoch)
			}

			delete(a.activeEpochs, status.epoch)

			active := len(a.activeEpochs)
			a.mutex.Unlock()

			if active == 0 {
				log.Infof("No more active epochs, terminating")
				return nil
			}

			log.Infof("%d active epochs running", active)

		case <-ctx.Done():
		# terminate
			a.terminate()
			log.Info("Agent has successfully terminated")
			return nil
		}
	}
}
	
	
	
	# 调用enovy的API 运行envoy 动态加载配置
func doEnvoyPost(path, contentType, body string, adminPort uint32) (*bytes.Buffer, error) {
	requestURL := fmt.Sprintf("http://127.0.0.1:%d/%s", adminPort, path)
	buffer, err := doHTTPPost(requestURL, contentType, body)
	if err != nil {
		return nil, err
	}
	return buffer, nil
}

```


 ### 在istio启动时的日志及设置链的规则如下

```bash

-- Logs begin at 一 2021-07-19 08:59:42 UTC, end at 一 2021-07-19 16:01:01 UTC. --
7月 19 09:03:35 localhost.localdomain systemd[1]: Started istio-sidecar: The Istio sidecar.
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -D PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -D PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -D OUTPUT -p tcp -j ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -F ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -X ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -F ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -X ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -F ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -X ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -F ISTIO_DIVERT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -X ISTIO_DIVERT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -F ISTIO_TPROXY
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t mangle -X ISTIO_TPROXY
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -F ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -X ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -F ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables -t nat -X ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -D PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -D PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -D OUTPUT -p tcp -j ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -F ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -X ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -F ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -X ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -F ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -X ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -F ISTIO_DIVERT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -X ISTIO_DIVERT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -F ISTIO_TPROXY
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t mangle -X ISTIO_TPROXY
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -F ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -X ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -F ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables -t nat -X ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables-save
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by iptables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *mangle
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :FORWARD ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by iptables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *nat
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables-save
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by ip6tables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *mangle
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :FORWARD ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by ip6tables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *nat
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: Environment:
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ------------
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ENVOY_PORT=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_CAPTURE_PORT=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_INBOUND_INTERCEPTION_MODE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_INBOUND_TPROXY_MARK=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_INBOUND_TPROXY_ROUTE_TABLE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_INBOUND_PORTS=8080
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_OUTBOUND_PORTS=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_LOCAL_EXCLUDE_PORTS=15090,15021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_SERVICE_CIDR=*
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ISTIO_SERVICE_EXCLUDE_CIDR=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: Variables:
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ----------
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: PROXY_PORT=15001
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: PROXY_INBOUND_CAPTURE_PORT=15006
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: PROXY_TUNNEL_PORT=15008
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: PROXY_UID=997
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: PROXY_GID=997
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_INTERCEPTION_MODE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_TPROXY_MARK=1337
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_TPROXY_ROUTE_TABLE=133
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_PORTS_INCLUDE=8080
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: INBOUND_PORTS_EXCLUDE=15090,15021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: OUTBOUND_IP_RANGES_INCLUDE=*
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: OUTBOUND_IP_RANGES_EXCLUDE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: OUTBOUND_PORTS_INCLUDE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: OUTBOUND_PORTS_EXCLUDE=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: KUBEVIRT_INTERFACES=
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ENABLE_INBOUND_IPV6=false
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: DNS_SERVERS=[10.10.10.6],[]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: Writing following contents to rules file:  /tmp/iptables-rules-1626685415906987113.txt145448538
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: * nat
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -N ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -N ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -N ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -N ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_INBOUND -p tcp --dport 15008 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_REDIRECT -p tcp -j REDIRECT --to-ports 15001
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_IN_REDIRECT -p tcp -j REDIRECT --to-ports 15006
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_INBOUND -p tcp --dport 8080 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p tcp -j ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo -s 127.0.0.6/32 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo ! -d 127.0.0.1/32 -p tcp ! --dport 53 -m owner --uid-owner 997 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo -p tcp ! --dport 53 -m owner ! --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -m owner --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo ! -d 127.0.0.1/32 -m owner --gid-owner 997 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo -p tcp ! --dport 53 -m owner ! --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -m owner --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -p tcp --dport 53 -d 10.10.10.6/32 -j REDIRECT --to-ports 15053
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -d 127.0.0.1/32 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -j ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p udp --dport 53 -m owner --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p udp --dport 53 -m owner --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p udp --dport 53 -d 10.10.10.6/32 -j REDIRECT --to-port 15053
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables-restore --noflush /tmp/iptables-rules-1626685415906987113.txt145448538
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: Writing following contents to rules file:  /tmp/ip6tables-rules-1626685415940193066.txt564611057
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: ip6tables-restore --noflush /tmp/ip6tables-rules-1626685415940193066.txt564611057
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: iptables-save
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by iptables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *mangle
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :FORWARD ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Generated by iptables-save v1.4.21 on Mon Jul 19 17:03:35 2021
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: *nat
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :PREROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :INPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :OUTPUT ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :POSTROUTING ACCEPT [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :ISTIO_INBOUND - [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :ISTIO_IN_REDIRECT - [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :ISTIO_OUTPUT - [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: :ISTIO_REDIRECT - [0:0]
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A PREROUTING -p tcp -j ISTIO_INBOUND
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p tcp -j ISTIO_OUTPUT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p udp -m udp --dport 53 -m owner --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -p udp -m udp --dport 53 -m owner --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A OUTPUT -d 10.10.10.6/32 -p udp -m udp --dport 53 -j REDIRECT --to-ports 15053
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_INBOUND -p tcp -m tcp --dport 15008 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_INBOUND -p tcp -m tcp --dport 8080 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_IN_REDIRECT -p tcp -j REDIRECT --to-ports 15006
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -s 127.0.0.6/32 -o lo -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT ! -d 127.0.0.1/32 -o lo -p tcp -m tcp ! --dport 53 -m owner --uid-owner 997 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo -p tcp -m tcp ! --dport 53 -m owner ! --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -m owner --uid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT ! -d 127.0.0.1/32 -o lo -m owner --gid-owner 997 -j ISTIO_IN_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -o lo -p tcp -m tcp ! --dport 53 -m owner ! --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -m owner --gid-owner 997 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -d 10.10.10.6/32 -p tcp -m tcp --dport 53 -j REDIRECT --to-ports 15053
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -d 127.0.0.1/32 -j RETURN
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_OUTPUT -j ISTIO_REDIRECT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: -A ISTIO_REDIRECT -p tcp -j REDIRECT --to-ports 15001
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: COMMIT
7月 19 09:03:35 localhost.localdomain istio-start.sh[6717]: # Completed on Mon Jul 19 17:03:35 2021
```