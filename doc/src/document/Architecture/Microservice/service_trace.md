链路追踪服务（service-trace）提供了 SequoiaCM 集群中请求的全链路追踪能力，支持将一次分布式请求还原成调用链路，并进行可视化展示，以帮助完成故障定位和性能分析，其具体架构如下：

![链路追踪架构][service_trace]


开启链路追踪
----

链路追踪功能默认关闭，如需打开，请为网关节点加入如下配置：

```lang-javascript
scm.trace.enabled=true
```

开启后，默认只会对 10% 的请求进行采样；如需修改，可通过如下网关节点配置进行调整（一般来说，采样率越高，产生的性能影响越大）：

```lang-javascript
# 调整采样率为50%（可选值为[0,100]）
scm.trace.samplePercentage=50
```

**Note**
>
> - 以上配置支持[动态更新][reload_config]
>

查看链路信息
----
开启链路追踪后，可通过浏览器访问链路追踪服务 ip:port 查看链路信息：
![链路信息列表][service_trace_list]

**Note**
>
> - 本页面展示了链路追踪服务采集到的链路信息列表，每一个列表项表示一次请求，也称为链路（Trace），链路中每途径的一个服务节点被称为 Span
>

可通过查询条件（服务名、请求时间等）对链路信息进行过滤，例如查询经过配置服务（config-server）的链路信息：
![链路信息过滤][service_trace_filter]

上述筛选出满足条件的一条链路信息，该链路信息的具体含义解释如下：
![链路信息列表项][service_trace_list_item]


- 第一行：318.488ms 表示整个链路的执行时间，3 spans 表示该链路途径了三个服务节点，即产生了三次调用
- 第二行：config-server 38% 表示调用配置服务节点的最大耗时占总链路时间的 38%
- 第三行：表示该链路所涉及的全部服务及各服务的处理耗时，如果同一服务调用了多次，这里显示的是最大耗时时间


点击链路信息列表中的具体链路，可查看该链路的详细信息（链路持续时间、链路途径的服务节点、各阶段的处理耗时等）：
![链路详细信息][service_trace_detail]

**Note**
>
> - 上述页面展示了一个创建工作区请求的具体链路信息：该请求总耗时 318.488ms，请求首先经过网关服务节点，网关调用内容服务节点，内容服务节点调用配置服务节点
> - 页面中节点的层级关系表示节点间的调用关系和调用顺序，长条的宽度表示该节点的处理耗时
> - 在该链路中，内容服务节点、配置服务节点被标为红色，表示在调用时出现了异常
>

点击链路信息中具体的服务节点，可查看本次调用的详细信息：
![调用详细信息][service_trace_span]

详细信息由上下两个表格组成，上方的表格表示本次调用的关键事件，表格中各字段的解释如下：

- Date Time：事件触发的具体时间
- Relative Time：时间触发时相对于整条链路开始时经过的时间
- Annotation：事件的类型，其中 Client Send 表示上游服务发起请求；Server Received 表示下游服务收到了请求；Server Send 表示下游服务发送了响应；ssa 表示下游服务发送了全部响应内容；Client Received 表示客户端收到了响应内容
- Address：表示触发事件的节点地址

下方表格展示了本次调用收集的一些额外信息，如请求方式、请求地址等，如果本次调用出现了异常，则异常信息也会展示在额外信息中

[service_trace]:Architecture/Microservice/service_trace.png
[service_trace_list]:Architecture/Microservice/service_trace_list.png
[service_trace_list_item]:Architecture/Microservice/service_trace_list_item.png
[service_trace_detail]:Architecture/Microservice/service_trace_detail.png
[service_trace_span]:Architecture/Microservice/service_trace_span.png
[service_trace_filter]:Architecture/Microservice/service_trace_filter.png
[reload_config]:Development/Java_Driver/reload_conf_opration.md