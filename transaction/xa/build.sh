#!/usr/bin/env bash

set -e

echo  "GOOS=linux go build"
mvn clean install

echo "-> HOST="$HOST
echo "-> TAG="v1
echo "-> USER="$USER
docker build -t registry.cn-shenzhen.aliyuncs.com/solarmesh/demo-nacos:v1 .

docker push registry.cn-shenzhen.aliyuncs.com/solarmesh/demo-nacos:v1