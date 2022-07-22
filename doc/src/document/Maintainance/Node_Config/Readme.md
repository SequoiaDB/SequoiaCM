SequoiaCM 服务包括 Spring Cloud 服务和内容管理核心服务，各个服务由其相应的服务节点来实现和承载，不同的服务节点拥有各自的配置参数。

1. Spring Cloud 服务列表：

 |服务类型|英文名             | 说明                       |
 |--------|------------------ |----------------------------| 
 |网关    | Gateway           |负责请求路由和负载均衡功能  |
 |注册中心| Service Center    |负责服务发现和注册功能      |
 |认证服务| Auth Server       |负责用户权限、会话管理功能  |
 |监控服务| Admin Server      |负责监控功能                |
 |服务跟踪| Service Trace     |全链路服务跟踪              |

2. 核心服务列表：

 |服务类型    |英文名             | 说明                       |
 |------------|------------------ |----------------------------|
 |内容管理服务| Content Server    |负责文件、目录、批次管理    |
 |调度服务    | Schedule Server   |负责文件迁移、清理等功能    |
 |配置服务    | Config Server     |负责工作区配置管理等功能    |
 |全文检索服务| Fulltext Server   |负责全文检索功能            |
 |OM 服务     | OM Server         |负责WEB页面管理功能         |
 |S3 服务     | S3 Server         |负责提供 S3 协议接口功能    |

##公共配置##

公共配置为所有节点均有的配置，统一说明如下：

|配置项       |类型   |说明                           |
|-------------|-------|-------------------------------|
|server.port|num|节点的对外提供服务的端口号|
|eureka.client.register-with-eureka|boolean|是否将本节点注册至服务中心，默认值：false|
|eureka.client.fetch-registry|boolean|本节点是否从注册中心获取其它节点的注册信息，默认值：false|
|eureka.client.region|str|此实例所在的区域|
|eureka.client.availability-zones|str|配置region下可用zone，如：zone1,zone2，表示region下有两个可用zone|
|eureka.client.service-url|str|配置zone的注册中心地址，如：http://192.168.31.90:8800/eureka/|
|eureka.instance.metadata-map.zone|str|注册到服务中心时，向其它实例描述自身所属的zone|
|eureka.client.prefer-same-zone-eureka|boolean|是否优先向同一个zone的服务中心注册，默认值：true|
|eureka.instance.hostname|str|描述此实例所在机器的主机名，用于节点间通信，不配置则通过一定的规则自动获取，在多网卡/IP环境下建议手动指定主机名|
|spring.cloud.client.hostname|str|描述此实例所在机器的主机名，用于生成节点 ID，不配置则通过一定的规则自动获取，在多网卡/IP环境下建议手动指定主机名；此配置项应该与 eureka.instance.hostname 同时配置|
|eureka.instance.ip-address|str|描述此实例所在机器的 IP 地址，不配置则通过一定的规则自动获取，在多网卡/IP环境下建议手动指定 IP 地址|
|spring.zipkin.enabled|boolean|是否开启服务跟踪，默认 false|
|spring.zipkin.base-url|str|配置服务跟踪节点的地址，如：http://192.168.31.90:8890|
|ribbon.MaxAutoRetries|num|对同一个实例请求的最大重试次数（不包括第一次），默认值：0|
|ribbon.MaxAutoRetriesNextServer|num|请求失败时，更换下一个实例进行重试，该参数表示最大更换次数（不包含第一个实例），默认值：1|
|ribbon.OkToRetryOnAllOperations|boolean|是否所有请求都进行重试，默认值：false|
|ribbon.ConnectTimeout|num|使用Ribbon时的连接建立超时时间，默认值：10000，单位：ms|
|ribbon.ReadTimeout|num|使用Ribbon时的读超时，默认值：30000，单位：ms|
|scm.feign.connectTimeout|num|使用Feign调用远程服务的连接建立超时时间，单位：ms。如果不设置，默认会使用ribbon.ConnectTimeout的值。|
|scm.feign.readTimeout|num|使用Feign调用远程服务的读超时，单位：ms。如果不设置，默认会使用ribbon.ReadTimeout的值。|
|hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests|num|配置分配给单个下游服务的信号量资源。调用下游服务前，会先获取信号量，调用结束后归还信号量，如获取不到，则调用会被拒绝。此配置限制了调用某个下游服务的最大并发数量。|
|hystrix.command.default.circuitBreaker.enabled|boolean|是否开启熔断保护，默认值：true。|
| hystrix.command.default.metrics.rollingStats.timeInMilliseconds |num| 熔断统计的时间窗口，默认值：10000，单位：ms。|
|hystrix.command.default.circuitBreaker.requestVolumeThreshold|num|熔断计算的最小样本数，只有在时间窗口（10s）内，调用某个服务的数量达到了该值，才会进行熔断判断，默认值：20。|
|hystrix.command.default.circuitBreaker.errorThresholdPercentage|num|触发熔断的失败率，当在时间窗口内调用某个服务的数量达到样本数，且失败率大于等于此值时，熔断器打开，后续对该服务的调用直接返回失败，默认值：50。|
|hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds|num|熔断器休眠窗口，当触发熔断一段时间后，尝试放行一个调用请求，根据该请求是否成功，来决定是继续熔断还是恢复正常，单位：ms，默认值：5000。|
|scm.hystrix.enabled|boolean|是否开启熔断与隔离能力，默认值：true。关闭后，该节点的熔断与隔离能力将失效，所有hystrix开头的配置项将不起作用。此配置在网关节点暂不生效。|
|management.port|num|配置节点的管理端口，用于系统状态监控。不配置时，默认为 server.port + 1，除 s3-server 外的其它服务均支持将 management.port 设为与 server.port 一致。|
|management.security.enabled|boolean|是否开启 Actuator 端点的权限控制，默认 false 。详情请见：[Actuator 安全性配置][actuator]|
|scm.ribbon.localPreferred|boolean|服务调用时是否优先选择同一台机器上的节点，默认值：true 。|

##公共配置举例##

SequoiaCM 可以配置指定

1. 配置 SequoiaCM 系统全局均在同一个 zone 内。

 ```
server.port=8080
eureka.client.region=beijing
eureka.client.availability-zones.beijing=zone1
eureka.client.service-url.zone1=http://192.168.31.90:8800/eureka/
eureka.instance.metadata-map.zone=zone1
eureka.client.prefer-same-zone-eureka=true
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
spring.zipkin.enabled=false
 ```

 > **Note:**
 >
 > * 全局 region 为 beijing , region 下只有一个名为 zone1 的 zone。
 >
 > * 需要指定 zone1 的注册中心地址（eureka.client.service-url.zone1）
 >
 > * 指定本身节点所在的 zone 为 zone1 （eureka.instance.metadata-map.zone）

2. 配置 SequoiaCM 系统全局有两个 zone: zone1和zone2 。节点分为两个 zone 之后，可以实现请求分流。从 zone1 所在节点过来的请求优先走 zone1 的节点；从 zone2 所在节点过来的请求优先走 zone2 的节点。

 ```
server.port=8080
eureka.client.region=beijing
eureka.client.availability-zones.beijing=zone1,zone2
eureka.client.service-url.zone1=http://192.168.31.90:8800/eureka/
eureka.client.service-url.zone2=http://192.168.32.90:8800/eureka/
eureka.instance.metadata-map.zone=zone1
eureka.client.prefer-same-zone-eureka=true
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
spring.zipkin.enabled=false
 ```

 > **Note:**
 >
 > * 全局 region 为 beijing , region 下有两个 zone：zone1,zone2 。
 >
 > * 需要分别指定 zone1 的注册中心地址（eureka.client.service-url.zone1）和 zone2 的注册中心地址（eureka.client.service-url.zone2）
 >
 > * 指定本身节点所在的 zone 为 zone1 （eureka.instance.metadata-map.zone）

[actuator]:Maintainance/Security/Security_Config/actuator.md