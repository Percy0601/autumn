# Autumn Framework
Autumn Framework(秋实框架)是一个服务治理框架，
在Apache Thrift协议之上能够快速在Spring Boot上进行开发的一个框架。

## 特性
### 1、轻量易用(Simple&Light)
只依赖apache-thrift和spring-boot-core模块

### 2、本地镜像(Native Image)
- 框架的实现方式采用零反射的静态代理方式(JSR-269)
- apache-thrift支持本地镜像
- spring-boot3.x支持本地镜像

### 3、性能好(Performance)
基于性能优良的apache-thrift特性，单线程达到10K，多线程模式下超10W+

### 4、云原生支持(Cloud Native)
- 在Kubernetes的服务注册之上，提供服务发现能力。
- 数据面与控制面分离
