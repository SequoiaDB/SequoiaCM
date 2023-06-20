##网关##
公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

Spring Cloud 配置

|配置项| 类型| 说明|
|------|-----|-----|
|spring.http.multipart.maxFileSize|str|文件上传大小。例如：4096Mb，默认值：1Mb|
|spring.http.multipart.maxRequestSize|str|设置一次请求总上传的数据大小。例如：4096Mb，默认值：10Mb|
|spring.http.multipart.fileSizeThreshold|str|设置文件刷盘的阈值。例如：1Mb，默认值：0|

SequoiaCM 配置


|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|scm.uploadForward.connectionTimeToLive|num|文件转发连接池的空闲连接保留时间，默认值：900，单位：秒|重启生效|
|scm.uploadForward.maxTotalConnections|num|文件转发连接池的最大连接数，默认值：1000|重启生效|
|scm.uploadForward.maxPerRouteConnections|num|文件转发连接池中，每个地址最大连接数，默认值：50|重启生效|
|scm.uploadForward.connectionCleanerRepeatInterval|num|文件转发连接池的空闲连接清理周期，默认值：30000，单位：毫秒|重启生效|
|scm.uploadForward.connectTimeout|num|文件转发连接池中的连接建连超时，不配置时使用 ribbon.ConnectTimeout 配置项指定的值，默认：10000，单位：毫秒|重启生效|
|scm.uploadForward.connectionRequestTimeout|num|从文件转发连接池中获取连接的超时时间，默认值：-1（表示不超时），单位：毫秒|重启生效|
|scm.uploadForward.socketTimeout|num|文件转发连接池中的连接读超时，不配置时使用 ribbon.ReadTimeout 配置项指定的值，默认：30000，单位：毫秒|重启生效|
|scm.statistics.types|str|需要进行统计的请求类型，可选值：file_upload（文件上传）、file_download（文件下载），需要统计多项时用逗号分开，默认不进行任何统计|在线生效|
|scm.statistics.types.file_upload.conditions.workspaces|str|统计文件上传请求的工作区过滤条件，填写工作区名字，多个工作区用逗号分开，未指定任何工作过滤条件时默认统计所有工作区的文件上传|在线生效|
|scm.statistics.types.file_upload.conditions.workspaceRegex|str|统计文件上传请求的工作区过滤条件，填写工作区名字正则表达式，未指定任何工作过滤条件时默认统计所有工作区的文件上传|在线生效|
|scm.statistics.types.file_download.conditions.workspaces|str|统计文件下载请求的工作区过滤条件，填写工作区名字，多个工作区用逗号分开，未指定任何工作过滤条件时默认统计所有工作区的文件下载|在线生效|
|scm.statistics.types.file_download.conditions.workspaceRegex|str|统计文件下载请求的工作区过滤条件，填写工作区名字正则表达式，未指定任何工作过滤条件时默认统计所有工作区的文件下载|在线生效|
|scm.statistics.rawDataCacheSize|num|网关用于缓存统计原始数据的队列大小，默认5000条原始数据|在线生效|
|scm.statistics.rawDataReportPeriod|num|网关每隔多长时间将统计原始数据上报给监控服务，默认值：10000，单位：毫秒|在线生效|
|scm.s3.userAgent|str|网关会借助 User-Agent 头来识别 S3 请求，当请求头包含该配置的指定值时，将会被网关识别为 S3 请求，默认值：aws-sdk-java，该配置允许配置多个，使用逗号进行分割|重启生效|
|scm.s3.chooserRefreshInterval|num|网关在决策 S3 请求发往哪个站点时，会依赖自身构筑的集群信息缓存，此配置控制缓存的刷新间隔，默认值：180000，单位：毫秒|重启生效|
|scm.trace.enabled|boolean|是否开启链路追踪功能，默认值：false|在线生效|
|scm.trace.samplePercentage|num|配置链路追踪的请求采样率，默认值：10（表示采样 10% 的请求），可选值：[0,100]|在线生效|
|scm.trace.sampleServices|str|配置链路追踪采样的下游服务列表，多个用逗号隔开，不存在该配置时默认对所有下游服务采样|在线生效|
|scm.conf.client.workspace.heartbeatInterval|num|配置工作区版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.client.bucket.heartbeatInterval|num|配置bucket版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|

##注册中心##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.store.sequoiadb.urls|str|认证服务信息存储在 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|重启生效|
|scm.store.sequoiadb.username|str|登录 SequoiaDB 的用户名。例如：sdbadmin，默认用户名为空|重启生效|
|scm.store.sequoiadb.password|str|登录 SequoiaDB 的密码文件。例如：/opt/scm-cloud/sdb.passwd，默认密码为空|重启生效|
|scm.store.sequoiadb.connectTimeout|num|连接超时时间，默认值：10000，单位：毫秒|重启生效|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num|最大重连间隔，默认值：15000，单位：毫秒|重启生效|
|scm.store.sequoiadb.socketTimeout|num|socket 超时时间，默认值：0（不检测超时），单位：毫秒|重启生效|
|scm.store.sequoiadb.useNagle|bool|是否开启 Nagle，默认值：false|重启生效|
|scm.store.sequoiadb.useSSL|bool|是否使用 SSL 安全连接，默认值：false|重启生效|
|scm.store.sequoiadb.maxConnectionNum|num|SequoiaDB 连接池最大连接数，默认值：500|重启生效|
|scm.store.sequoiadb.deltaIncCount|num|当需要新增连接时，一次新增的连接数，默认值：10|重启生效|
|scm.store.sequoiadb.maxIdleNum|num|最大空闲连接数，默认值：2|重启生效|
|scm.store.sequoiadb.keepAliveTime|num|连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|重启生效|
|scm.store.sequoiadb.recheckCyclePeriod|num|清理空闲连接的间隔时间。默认值：30000，单位：毫秒|重启生效|
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性，默认值：true|重启生效|
|scm.store.sequoiadb.connectStrategy|str|元数据服务 SequoiaDB 连接池连接策略，可选值(SERIAL, RANDOM, LOCAL, BALANCE)。默认值：SERIAL|重启生效|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|


##认证服务##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.store.sequoiadb.urls|str|认证服务信息存储在 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|重启生效|
|scm.store.sequoiadb.username|str|登录 SequoiaDB 的用户名。例如：sdbadmin，默认用户名为空|重启生效|
|scm.store.sequoiadb.password|str|登录 SequoiaDB 的密码文件。例如：/opt/scm-cloud/sdb.passwd，默认密码为空|重启生效|
|scm.store.sequoiadb.connectTimeout|num|连接超时时间，默认值：10000，单位：毫秒|重启生效|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num|最大重连间隔，默认值：15000，单位：毫秒|重启生效|
|scm.store.sequoiadb.socketTimeout|num|socket 超时时间，默认值：0（不检测超时），单位：毫秒|重启生效|
|scm.store.sequoiadb.useNagle|bool|是否开启 Nagle，默认值：false|重启生效|
|scm.store.sequoiadb.useSSL|bool|是否使用 SSL 安全连接，默认值：false|重启生效|
|scm.store.sequoiadb.maxConnectionNum|num|SequoiaDB 连接池最大连接数，默认值：500|重启生效|
|scm.store.sequoiadb.deltaIncCount|num|当需要新增连接时，一次新增的连接数，默认值：10|重启生效|
|scm.store.sequoiadb.maxIdleNum|num|最大空闲连接数，默认值：10|重启生效|
|scm.store.sequoiadb.keepAliveTime|num|连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|重启生效|
|scm.store.sequoiadb.recheckCyclePeriod|num|清理空闲连接的间隔时间。默认值：30000，单位：毫秒|重启生效|
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性，默认值：true|重启生效|
|scm.store.sequoiadb.connectStrategy|str|元数据服务 SequoiaDB 连接池连接策略，可选值(SERIAL, RANDOM, LOCAL, BALANCE)。默认值：SERIAL|重启生效|
|scm.store.sequoiadb.location|str|元数据服务 SequoiaDB 连接池 location 配置，默认值为空|重启生效|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|scm.auth.token.enabled|bool|是否开启token用户登录，默认值：false|重启生效|
|scm.auth.token.allowAnyValue|bool|是否允许任意token，默认值：false|重启生效|
|scm.auth.token.tokenValue|str|允许登录的 token 值|重启生效|
|scm.audit.mask|str|审计日志操作配置，控制对不同操作命令记录审计日志。默认为空，不记录任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.audit.userMask|str|审计日志用户配置，默认为空，不记录任何用户的审计日志。支持的配置详见[审计用户类型掩码列表][audit_log]|在线生效|
|scm.audit.user.xxx|str|审计日志操作配置，控制具体用户对不同操作命令记录审计日志。默认为空，不记录用户名为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.audit.userType.xxx|str|审计日志操作配置，控制用户类型对不同操作命令记录审计日志。默认为空，不记录用户类型为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.session.maxInactiveInterval|num|session 最大保持时间。默认值：1800，单位：秒|重启生效|
|scm.session.cleanInactiveInterval|num|清理过期 session 的记录。默认值：3600，单位：秒|重启生效|

 > **Note:**
 >
 > * scm.audit.user.xxx  xxx 表示用户的用户名，该配置项设置具体用户审计操作类型
 >
 > * scm.audit.userType.xxx  xxx 表示用户类型，该配置项设置用户类型审计操作类型，支持值详见[审计用户类型掩码列表][audit_log]
 
##监控服务##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|scm.store.sequoiadb.urls|str|认证服务信息存储在 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|重启生效|
|scm.store.sequoiadb.username|str|登录 SequoiaDB 的用户名。例如：sdbadmin，默认用户名为空|重启生效|
|scm.store.sequoiadb.password|str|登录 SequoiaDB 的密码文件。例如：/opt/scm-cloud/sdb.passwd，默认密码为空|重启生效|
|scm.store.sequoiadb.connectTimeout|num|连接超时时间，默认值：10000，单位：毫秒|重启生效|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num|最大重连间隔，默认值：15000，单位：毫秒|重启生效|
|scm.store.sequoiadb.socketTimeout|num|socket 超时时间，默认值：0（不检测超时），单位：毫秒|重启生效|
|scm.store.sequoiadb.useNagle|bool|是否开启 Nagle，默认值：false|重启生效|
|scm.store.sequoiadb.useSSL|bool|是否使用 SSL 安全连接，默认值：false|重启生效|
|scm.store.sequoiadb.maxConnectionNum|num|SequoiaDB 连接池最大连接数，默认值：500|重启生效|
|scm.store.sequoiadb.deltaIncCount|num|当需要新增连接时，一次新增的连接数，默认值：10|重启生效|
|scm.store.sequoiadb.maxIdleNum|num|最大空闲连接数，默认值：10|重启生效|
|scm.store.sequoiadb.keepAliveTime|num|连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|重启生效|
|scm.store.sequoiadb.recheckCyclePeriod|num|清理空闲连接的间隔时间。默认值：30000，单位：毫秒|重启生效|
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性。默认值：true|重启生效|
|scm.store.sequoiadb.connectStrategy|str|元数据服务 SequoiaDB 连接池连接策略，可选值(SERIAL, RANDOM, LOCAL, BALANCE)。默认值：SERIAL|重启生效|
|scm.store.sequoiadb.location|str|元数据服务 SequoiaDB 连接池 location 配置，默认值为空|重启生效|
|scm.statistics.job.firstTime|str|监控服务节点启动后，首次执行统计任务的时间。默认值：00:00:00，格式：HH:mm:ss|重启生效|
|scm.statistics.job.period|str|执行统计任务的间隔时间。默认值：1d（一天，只支持按天统计）|重启生效|
|scm.server.listInstanceCheckInterval|num|监控服务节点在处理list请求时，每列取多少个对象检查一次与客户端的连接状态（检查状态的同时，会对该连接执行 flush ），默认值：2000|重启生效|
|scm.statistics.timeGranularity|str|监控服务对网关上报的统计数据的处理粒度，可选值：DAY、HOUR，默认值：DAY，该粒度体现在用户按时间段检索统计数据时，时间段所允许的最大精度|在线生效|
|scm.statistics.job.breakpointFileCleanPeriod|num|清理断点文件上传时间临时记录表的周期。默认值：7d（每隔7天清理一次）|重启生效|
|scm.statistics.job.breakpointFileStayDays|num|断点文件上传时间记录在数据表中的最大遗留时间（当断点文件上传完成但未转换成普通文件时，表中会临时保存该文件的上传时间）。默认值：10d|重启生效|
|scm.zookeeper.cleanJobPeriod                |num|配置服务节点全量清理zookeeper无效节点的周期，默认值：1800000 (30分钟)，单位：毫秒                           |重启生效|
|scm.zookeeper.maxBuffer                     |num|配置服务节点全量清理zookeeper无效节点时所使用的最大buffer大小，默认使用 JVM 最大堆内存的 1/5，单位：字节       |重启生效|
|scm.zookeeper.cleanJobResidualTime          |num|配置服务节点将清理残留多久的zookeeper节点，默认值：180000 (3分钟)，单位：毫秒                              |重启生效|
|scm.zookeeper.maxCleanThreads               |num|配置服务节点清理残留的zookeeper节点所使用的最大线程数，默认值：6                                         |重启生效|
|scm.zookeeper.cleanQueueSize                |num|配置服务节点清理残留的zookeeper节点所使用的异步缓存队列的大小，默认值：10000                                |重启生效|
|scm.zookeeper.acl.enabled     | boolean  | 是否开启 ZooKeeper ACL 权限控制，默认值：false。详情请见：[ZooKeeper 安全性配置][zookeeper_sercurity]|重启生效|
|scm.zookeeper.acl.id          | str   | 授权对象，填写用户名密码串（username:password）的加密文件路径|重启生效|
|scm.quota.sync.maxTimeDiff         | num   | 额度同步时，允许各节点间的最大时间差，单位为毫秒，默认值为 30000 |在线生效|
|scm.quota.sync.maxConcurrentCount        | num   | 进行额度同步的最大并发数，默认值为 30 |在线生效|
|scm.quota.sync.retryInterval        | num   | 扫描额度统计表进行重试的周期，单位为秒，默认值为 300 |在线生效|
|scm.quota.sync.expireTime       | num   | 内容服务等待更新统计进度的时间阈值，如果在该阈值内未更新，则视为更新失败，单位为秒，默认值为 300 |在线生效|
|scm.conf.client.workspace.heartbeatInterval|num|配置工作区版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.client.bucket.heartbeatInterval|num|配置bucket版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.client.user.heartbeatInterval|num|配置用户版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.client.role.heartbeatInterval|num|配置角色版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|

##服务跟踪##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|zipkin.storage.mem.max-spans|num|配置链路追踪服务所存储的最大 span 数量，默认值：50000，超过大小时会丢弃最旧的 span|重启生效|

[public_config]:Maintainance/Node_Config/Readme.md
[audit_log]:Maintainance/Diaglog/audit_log.md
[zookeeper_sercurity]:Maintainance/Security/Security_Config/zookeeper.md