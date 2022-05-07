SequoiaCM 支持监控内容服务节点的请求处理耗时，以及该请求涉及的各项内部操作耗时，当请求处理时间或内部操作的执行时间达到用户设置的阈值时，会在日志文件中打印慢操作告警信息。

## 存储配置 ##

内容服务节点安装目录下的 conf 目录，包含节点的日志配置文件 logback.xml，其中 slowlogTOFILE 为慢操作日志记录至文件的 appender。可以通过修改这个 appender
的属性改变日志输出配置，默认情况下慢操作日志会输出到 slowlog.log 这个文件中。

## 慢操作配置 ##

慢操作日志通过节点的配置文件（application.properties）进行配置：

|配置项  |类型        |说明                           |
|--------------|----------------|-------------------------------|
|scm.slowlog.enabled         |boolean    |是否开启慢操作告警功能，默认值：false       |
|scm.slowlog.allRequest      |num        |配置全局所有请求的告警阈值，当某个请求的处理时间大于或等于此值时，会在日志文件中打印慢操作告警信息。 默认值：-1（负数表示无限大的告警阈值），单位：毫秒      |
|scm.slowlog.request.xxx     |num        |单独配置某类请求的告警阈值，xxx 填写请求方式和地址，如：scm.slowlog.request.[POST/api/v1/files]=10000 配置文件上传请求的告警阈值为 10 秒，由于请求地址中包含特殊字符，需要用 [ ] 包裹。下方的表格中列举了常见的操作及其请求路径 |
|scm.slowlog.allOperation    |num        |配置全局内部操作的告警阈值，当某个内部操作的执行时间大于或等于此值时，会在日志文件中打印慢操作告警信息。 默认值：-1（负数表示无限大的告警阈值），单位：毫秒    |
|scm.slowlog.operation.xxx   |num        |单独配置某个内部操作的告警阈值，xxx 填写具体的操作名称，当前具体支持的操作请见下方的内部操作说明列表   |

**常见操作及其请求路径说明列表**

|操作名称         |请求路径                        |
|---------------|-------------------------------|
|上传文件        |[POST/api/v1/files]            |
|下载文件        |[GET/api/v1/files/*]           |
|列取文件        |[GET/api/v1/files]             |
|更新文件        |[PUT/api/v1/files/*]           |
|获取某个文件     |[HEAD/api/v1/files/*/**]       |

**Note**
>
> - 以上配置支持[动态更新][reload_config]
> 
> - 支持使用通配符，* 表示匹配0个或多个字符，** 表示匹配多级目录
>

**内部操作说明列表**

|内部操作类型     |触发场景               |说明                            |
|---------------|---------------------|-------------------------------|
|openWriter     |文件上传               |打开数据存储对象（如打开 lob）     |
|writeData      |文件上传               |写数据到存储对象               |
|closeWriter    |文件上传               |关闭数据存储对象（如关闭 lob）   |
|preCreate      |文件上传               |文件上传前置处理               |
|postCreate     |文件上传               |文件上传后置处理              |
|accessMeta     |文件上传、下载          |读写元数据                   |
|openReader     |文件下载               |打开数据源                   |
|readData       |文件下载               |读取数据源                   |
|closeReader    |文件下载               |关闭数据源                   |
|acquireLock    |获取分布式锁            |获取分布式锁                 |
|releaseLock    |释放分布式锁            |释放分布式锁                 |
|feign          |远程调用               |远程调用                    |

## 慢操作告警信息 ##

具体告警日志包含的信息如下：

|审计信息               |说明                       |示例                                             |
|----------------------|--------------------------|-------------------------------------------------|
|sessionId             |发起请求用户的 sessionId     |d73d3bf9-ea22-4f19-b620-0a567dca5661                                       |
|spend                 |本次请求总的处理时间          |135ms                                       |
|method                |请求方式                    |POST                                 |
|path                  |请求路径                    |/api/v1/files/?workspace_name=ws_default                               |
|start                 |请求开始时间                 |2022-04-26 11:19:47.108                                     |
|end                   |请求结束时间                 |2022-04-26 11:19:47.243                                     |
|readClientSpend       |读取客户端请求体的时间         |86ms                                     |
|writeResponseSpend    |写客户端响应体的时间           |0ms                                     |
|operations            |该请求涉及的关键内部操作耗时信息  |[*accessMeta=27ms, closeWriter=2ms, openWriter=4ms, postCreate=0ms, preCreate=0ms, writeData(16)=9ms] |
|extra                 |额外信息，用于辅助问题定位       |[fileId=626764d34000010096eb65b9] |

**Note**
>
> - 在关键内部操作耗时信息中，若某个操作的执行时间达到用户设置的阈值，会使用 * 标注，如：*accessMeta=27ms
>
> - 若某个操作执行了多次，会在括号中显示具体的执行次数，如：writeData(16)=9ms 表示 writeData 执行了16次


[reload_config]:Development/Java_Driver/reload_conf_opration.md
