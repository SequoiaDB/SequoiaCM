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

|配置项| 类型| 说明|
|------|-----|-----|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.uploadForward.connectionTimeToLive|num|文件转发连接池的空闲连接保留时间，默认值：900，单位：秒|
|scm.uploadForward.maxTotalConnections|num|文件转发连接池的最大连接数，默认值：1000|
|scm.uploadForward.maxPerRouteConnections|num|文件转发连接池中，每个地址最大连接数，默认值：50|
|scm.uploadForward.connectionCleanerRepeatInterval|num|文件转发连接池的空闲连接清理周期，默认值：30000，单位：毫秒|
|scm.uploadForward.connectTimeout|num|文件转发连接池中的连接建连超时，默认值：5000，单位：毫秒|
|scm.uploadForward.connectionRequestTimeout|num|从文件转发连接池中获取连接的超时时间，默认值：-1（表示不超时），单位：毫秒|
|scm.uploadForward.socketTimeout|num|文件转发连接池中的连接读超时，默认值：30000，单位：毫秒|

##注册中心##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明|
|------|-----|-----|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|


##认证服务##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明|
|------|-----|-----|
|scm.store.sequoiadb.urls|str|认证服务信息存储在 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|
|scm.store.sequoiadb.username|str|登录 SequoiaDB 的用户名。例如：sdbadmin，默认用户名为空|
|scm.store.sequoiadb.password|str|登录 SequoiaDB 的密码文件。例如：/opt/scm-cloud/sdb.passwd，默认密码为空|
|scm.store.sequoiadb.connectTimeout|num|连接超时时间，默认值：10000，单位：毫秒|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num|最大重连间隔，默认值：15000，单位：毫秒|
|scm.store.sequoiadb.socketTimeout|num|socket 超时时间，默认值：0（不检测超时），单位：毫秒|
|scm.store.sequoiadb.useNagle|bool|是否开启 Nagle，默认值：false|
|scm.store.sequoiadb.useSSL|bool|是否使用 SSL 安全连接，默认值：false|
|scm.store.sequoiadb.maxConnectionNum|num|SequoiaDB 连接池最大连接数，默认值：500|
|scm.store.sequoiadb.deltaIncCount|num|当需要新增连接时，一次新增的连接数，默认值：10|
|scm.store.sequoiadb.maxIdleNum|num|最大空闲连接数，默认值：10|
|scm.store.sequoiadb.keepAliveTime|num|连接池保留空闲连接的时长，默认值：0（不清除空闲连接），单位：毫秒|
|scm.store.sequoiadb.recheckCyclePeriod|num|清理空闲连接的间隔时间。默认值：60000，单位：毫秒|
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性，默认值：true|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.auth.token.enabled|bool|是否开启token用户登录，默认值：false|
|scm.auth.token.allowAnyValue|bool|是否允许任意token，默认值：false|
|scm.auth.token.tokenValue|str|允许登录的 token 值|
|scm.audit.mask|str|审计日志操作配置，控制对不同操作命令记录审计日志。默认为空，不记录任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.audit.userMask|str|审计日志用户配置，默认为空，不记录任何用户的审计日志。支持的配置详见[审计用户类型掩码列表][audit_log]|
|scm.audit.user.xxx|str|审计日志操作配置，控制具体用户对不同操作命令记录审计日志。默认为空，不记录用户名为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.audit.userType.xxx|str|审计日志操作配置，控制用户类型对不同操作命令记录审计日志。默认为空，不记录用户类型为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.session.maxInactiveInterval|num|session 最大保持时间。默认值：1800，单位：秒|
|scm.session.cleanInactiveInterval|num|清理过期 session 的记录。默认值：3600，单位：秒|

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

|配置项| 类型| 说明|
|------|-----|-----|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.store.sequoiadb.urls|str|认证服务信息存储在 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|
|scm.store.sequoiadb.username|str|登录 SequoiaDB 的用户名。例如：sdbadmin，默认用户名为空|
|scm.store.sequoiadb.password|str|登录 SequoiaDB 的密码文件。例如：/opt/scm-cloud/sdb.passwd，默认密码为空|
|scm.store.sequoiadb.connectTimeout|num|连接超时时间，默认值：10000，单位：毫秒|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num|最大重连间隔，默认值：15000，单位：毫秒|
|scm.store.sequoiadb.socketTimeout|num|socket 超时时间，默认值：0（不检测超时），单位：毫秒|
|scm.store.sequoiadb.useNagle|bool|是否开启 Nagle，默认值：false|
|scm.store.sequoiadb.useSSL|bool|是否使用 SSL 安全连接，默认值：false|
|scm.store.sequoiadb.maxConnectionNum|num|SequoiaDB 连接池最大连接数，默认值：500|
|scm.store.sequoiadb.deltaIncCount|num|当需要新增连接时，一次新增的连接数，默认值：10|
|scm.store.sequoiadb.maxIdleNum|num|最大空闲连接数，默认值：10|
|scm.store.sequoiadb.keepAliveTime|num|连接池保留空闲连接的时长，默认值：0（不清除空闲连接），单位：毫秒|
|scm.store.sequoiadb.recheckCyclePeriod|num|清理空闲连接的间隔时间。默认值：60000，单位：毫秒|
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性。默认值：true|
|scm.statistics.job.firstTime|str|监控服务节点启动后，首次执行统计任务的时间。默认值：00:00:00，格式：HH:mm:ss|
|scm.statistics.job.period|str|执行统计任务的间隔时间。默认值：1d（一天，只支持按天统计）|
|scm.server.listInstanceCheckInterval|num|监控服务节点在处理list请求时，每列取多少个对象检查一次与客户端的连接状态（检查状态的同时，会对该连接执行 flush ），默认值：2000|

##服务跟踪##

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明|
|------|-----|-----|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|

[public_config]:Maintainance/Node_Config/Readme.md
[audit_log]:Maintainance/Diaglog/audit_log.md