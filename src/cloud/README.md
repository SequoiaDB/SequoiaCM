SequoiaCM Cloud
===============

SequoiaCM Cloud是基于Spring Cloud开发的SequoiaCM微服务框架，包含以下服务：
1. Gateway（网关）
   基于Zuul实现服务网关，包括路由和负载均衡功能。
2. Service Center（服务中心）
   基于Eureka实现服务注册和发现。
3. Service Trace
   基于Zipkin实现全链路服务跟踪。
4. Admin Server
   基于Spring Boot Admin实现微服务的监控。

### Requirements ###

* JDK1.8
* Maven 3.3+

### Build ###

```
./dev.py
```

安装包生成目录：project/assembly/target/

### Deploy ###

部署到本地：

```
./dev.py --noup --nocompile --install
```

编译部署可以一次完成：
```
./dev.py --install
```

部署节点在deploy.json中配置，可以根据实际情况修改该文件。

### Configuration ###

1. Gateway

| option | default value |
| ------ | ------------- |
| server.port | 8080 |
| eureka.client.serviceUrl.defaultZone | http://localhost:8800/eureka/ |

2. Service Center

| option | default value |
| ------ | ------------- |
| server.port | 8800 |

3. Authentication Server

| option | default value |
| ------ | ------------- |
| server.port | 8810 |
| eureka.client.register-with-eureka | false |
| eureka.client.serviceUrl.defaultZone | http://localhost:8800/eureka/ |
| scm.auth.sequoiadb.urls | - |
| scm.auth.sequoiadb.username | - |
| scm.auth.sequoiadb.password | - |
| scm.auth.token.enabled | false |
| scm.auth.token.allowAnyValue | false |
| scm.auth.token.tokenValue | token123 |
| spring.ldap.urls | - |
| spring.ldap.username | - |
| spring.ldap.password | - |
| spring.ldap.base | - |
| spring.ldap.usernameAttribute | uid |

4. Service Trace

| option | default value |
| ------ | ------------- |
| server.port | 8890 |
| eureka.client.serviceUrl.defaultZone | http://localhost:8800/eureka/ |

5. Admin Server

| option | default value |
| ------ | ------------- |
| server.port | 8900 |
| eureka.client.serviceUrl.defaultZone | http://localhost:8800/eureka/ |
