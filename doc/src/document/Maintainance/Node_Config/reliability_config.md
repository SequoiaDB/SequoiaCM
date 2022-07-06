SequoiaCM3.2 版本后，为保障 SequoiaCM 集群的可靠性，基于微服务容错组件 Hystrix，引入了服务调用的隔离与熔断机制，本章节主要介绍与容错相关的配置参数及效果，以及在生产环境下的配置建议。

## 基本概念 ##

### 隔离 ###

SequoiaCM 集群由若干个微服务组成，一个微服务可能会依赖多个下游服务，若下游服务（被调用方）因为节点故障、网络延迟等原因导致响应缓慢，则上游服务（调用方）在调用时会阻塞在等待响应结果上，等待时会一直占用上游服务的工作线程资源。流量高峰期时，上游服务大量调用下游故障服务，上游服务的工作线程被大量占用。
![无隔离][reliability_no_isolate]
如上图所示，上游服务Web 容器中的线程资源被某个故障服务大量占用，此时对其它下游服务的调用已经没有足够的线程资源去处理。
![隔离][reliability_isolate]
在引入隔离机制后，为每个服务分配固定大小的信号量资源，调用下游服务前，必须先获取该服务对应的信号量才能发起调用，调用结束后释放信号量资源。当某个服务出现故障阻塞时，由于信号量大小的限制，不会使得上游服务的全部线程产生阻塞。通过服务间的相互隔离，当某个下游服务出现故障时，其它下游服务不受影响。

### 熔断 ###

当在短时间内调用下游服务出现大量失败时，上游服务熔断对下游服务的调用，采取快速失败机制，直接返回失败信息，不产生真实调用，从而起到快速响应和保护下游服务的作用。当熔断超过一定时间后，如果发现下游服务已经可用，则取消熔断，及时恢复下游服务。

## 配置项 ##

以下配置项适用于除 om-server 外的其它服务节点。

### 隔离配置 ###

| **配置项**                                                   | **默认值** | **说明**                                                     |
| ------------------------------------------------------------ | ---------- | ------------------------------------------------------------ |
| server.tomcat.max-threads                                    | 400        | 配置tomcat的最大线程数，此配置决定了当前节点的最大并发处理能力。当并发请求超过该值时，多余的请求会进入队列等待，直到有空闲线程再处理。 |
| hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests | -        | 配置分配给单个下游服务的信号量资源。调用下游服务前，会先获取信号量，调用结束后归还信号量，如获取不到，则调用会被拒绝。此配置限制了调用某个下游服务的最大并发数量，默认不做限制。 |

> **Note:** 
> 如需专门为某个下游服务配置信号量大小，可将配置项中的“default”用具体的服务名（小写）替代（服务名称请参考[服务列表][service_list]），如： 
>
> - 配置分配给调度服务的信号量大小为200：
>
> `hystrix.command.schedule-server.execution.isolation.semaphore.maxConcurrentRequests=200` 
>
> - 假设主站点的名称为 rootSite，配置分配给主站点的信号量大小为300：
>
> `hystrix.command.rootsite.execution.isolation.semaphore.maxConcurrentRequests=300`  

- 当调用某个下游服务的并发数量超过对应的信号量大小时，多余的调用会被拒绝，此参数值需要根据实际的节点并发来配置。

- 由于对下游服务的调用由上游服务的工作线程完成，信号量设置得越大，占用的最大工作线程数也就越多。为了使某个下游服务不占用全部工作线程，一般会将信号量设为小于最大线程数的值。

- 特别地，如果不需要限制对下游服务的调用并发数量，可将信号量设为大于等于最大线程数的值：

`hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests=${server.tomcat.max-threads}`

### 熔断配置 ###

| **配置项**                                                   | **默认值** | **说明**                                                     |
| ------------------------------------------------------------ | ---------- | ------------------------------------------------------------ |
| hystrix.command.default.circuitBreaker.enabled               | true       | 是否开启熔断保护。                                           |
| hystrix.command.default.metrics.rollingStats.timeInMilliseconds               | 10000       | 熔断统计的时间窗口，默认为10秒，单位：ms。|
| hystrix.command.default.circuitBreaker.requestVolumeThreshold | 20         | 熔断计算的最小样本数，只有在时间窗口（10s）内，调用某个服务的数量达到了该值，才会进行熔断判断。 |
| hystrix.command.default.circuitBreaker.errorThresholdPercentage | 50         | 触发熔断的失败率，当在时间窗口内调用某个服务的数量达到样本数，且失败率大于等于50%时，熔断器打开，后续对该服务的调用直接返回失败。 |
| hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds | 5000       | 熔断器休眠窗口，当触发熔断一段时间后，尝试放行一个调用请求，根据该请求是否成功，来决定是继续熔断还是恢复正常，单位：ms。 |

  **以上配置的效果是：**若在10s内调用某个服务达到了20次，并且有一半的请求失败（出现网络异常，如请求超时），则熔断对该服务的调用，熔断期间对该服务的调用会直接返回失败。触发熔断5s后，尝试放行一个调用请求，若本次请求成功，则取消熔断，否则继续熔断并等待下一个5s后继续检查。  

跟隔离类似，如需为某个下游服务单独设置隔离策略，可将配置项中的 “default” 换成具体的服务名（服务名称请参考[服务列表][service_list]）。



### 超时配置 ###

| **配置项**                      | **默认值** | **说明**                                             |
| ------------------------------- | ---------- | ---------------------------------------------------- |
| ribbon.ConnectTimeout           | 10000      | 配置调用下游服务时，与下游服务的连接超时，单位：ms。 |
| ribbon.ReadTimeout              | 30000      | 配置调用下游服务时的读超时，单位：ms。               |
| ribbon.MaxAutoRetries           | 0          | 调用下游服务失败时，对同一个节点的重试次数。         |
| ribbon.MaxAutoRetriesNextServer | 2          | 调用下游服务失败时，切换重试节点的个数。             |



### 其它配置 ###

| **配置项**          | **默认值** | **说明**                                                     |
| ------------------- | ---------- | ------------------------------------------------------------ |
| scm.hystrix.enabled | true       | 是否开启熔断与隔离能力，将此配置设为 false 后，该节点的熔断与隔离能力将失效，所有 hystrix 开头的配置项将不起作用。网关服务暂不支持关闭该功能。 |


## 生产环境配置建议 ##

假设当前服务在同一个 zone 中共有两个节点，整个系统的最大并发压力为800，则单个节点理论负载的并发为 800/2 = 400，此服务节点的隔离配置建议设置为：

``` 
hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests=400
## 配置分配给单个下游服务信号量资源为当前节点的所负载的理论并发数：400。

server.tomcat.max-threads=500
## 将最大线程数设为比理论负载并发数多25%。

hystrix.command.主站点名.execution.isolation.semaphore.maxConcurrentRequests=450
## 设置分配给主站点的信号量大小为450（大于理论负载并发数，小于最大线程数）
```

其它配置建议参考默认值。

## 附录 ##
### 服务列表 ###

| **服务类型**                      | **服务名称** |
| ------------------------------- | ---------- |
| 内容管理服务                      | 站点名称，如：rootsite      | 
| 配置服务                         | config-server            |
| 调度服务                         | schedule-server          |
| 注册中心                         | service-center           |
| 网关服务                         | gateway                  |
| 认证服务                         | auth-server              |
| 监控服务                         | admin-server             |
| 消息队列服务                      | mq-server                |
| 全文检索服务                      | fulltext-server          |
| S3 服务                         | 部署 s3 服务时自定义的服务名称 |


[reliability_no_isolate]:Maintainance/Node_Config/reliability_no_isolate.png
[reliability_isolate]:Maintainance/Node_Config/reliability_isolate.png
[service_list]:Maintainance/Node_Config/reliability_config.md#服务列表