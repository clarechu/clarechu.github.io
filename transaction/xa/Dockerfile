FROM openjdk:8-jre-alpine

MAINTAINER <CloudToGo>

ENV APP_BIN=/bin \
    PATH=${APP_BIN}:$PATH \
    TZ='Asia/Shanghai'

RUN  mkdir -p ${APP_BIN} ${APP_ROOT} \
     && sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories \
     && apk update \
     && apk upgrade \
     && apk --no-cache add ca-certificates iputils\
     && apk add -U tzdata ttf-dejavu busybox-extras curl bash git\
     && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

WORKDIR /app

COPY account-xa/target/*.jar /app/
COPY business-xa/target/*.jar /app/
COPY order-xa/target/*.jar /app/
COPY storage-xa/target/*.jar /app/
COPY eureka/target/*.jar /app/
COPY eureka-provider/target/*.jar /app/

MAINTAINER ["java", "-jar", "-Xms256m", "-Xmx256m"]