
公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项                     |类型   |说明                                    |
|---------------------------|-------|----------------------------------------|
|spring.application.name|str|内容服务节点的服务名，即所属的站点名|
|scm.server.transferCheckLength |num    |迁移任务，在某个文件迁移过程中，每迁移多少数据检查一次任务状态，默认值：10485760，单位：字节|
|scm.server.transferConnectTimeout|num  |迁移任务，源节点与目标节点建立连接的超时时间，默认值：3000，单位：毫秒|
|scm.server.transferReadTimeout|num |迁移任务，源节点与目标节点建立连接后，源节点的读超时，默认值：120000，单位：毫秒|
|scm.server.listInstanceCheckInterval|num|内容服务节点在处理list请求时，每列取多少个对象检查一次与客户端的连接状态（检查状态的同时，会对该连接执行 flush ），默认值：2000|
|scm.audit.mask|str|审计日志操作配置，控制对不同操作命令记录审计日志。默认为空，不记录任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.audit.userMask|str|审计日志用户配置，默认为空，不记录任何用户的审计日志。支持的配置详见[审计用户类型掩码列表][audit_log]|
|scm.audit.user.xxx|str|审计日志操作配置，控制具体用户对不同操作命令记录审计日志。默认为空，不记录用户名为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.audit.userType.xxx|str|审计日志操作配置，控制用户类型对不同操作命令记录审计日志。默认为空，不记录用户类型为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|
|scm.privilege.heartbeat.interval|num|内容服务节点查询鉴权服务更新自身权限信息的心跳间隔，默认值：10000，单位：毫秒|
|scm.conf.version.siteHeartbeat|num|内容服务节点请求查询配置服务site版本号的心跳间隔，默认值：180000，单位：毫秒|
|scm.conf.version.nodeHeartbeat|num|内容服务节点请求查询配置服务node版本号的心跳间隔，默认值：180000，单位：毫秒|
|scm.conf.version.workspaceHeartbeat|num|内容服务节点请求查询配置服务workspace版本号的心跳间隔，默认值：180000，单位：毫秒|
|scm.conf.version.metaDataHeartbeat|num|内容服务节点请求查询配置服务metaData版本号的心跳间隔，默认值：180000，单位：毫秒|
|scm.rootsite.meta.url          |str    |内容服务节点的元数据服务地址（sequoiadb的协调节点地址列表：ip1:port2,ip2:port2）|
|scm.rootsite.meta.user         |str    |内容服务节点的元数据服务用户名（sequoiadb的用户名），默认用户名为空|
|scm.rootsite.meta.password     |str    |内容服务节点的元数据服务密码文件路径（sequoiadb的密码），默认密码为空|
|scm.sdb.connectTimeout     |num    |内容服务节点与sdb建立连接的超时时长，默认值：10000，单位：毫秒|
|scm.sdb.maxAutoConnectRetryTime|num    |内容服务节点与sdb建立连接的重试时长，默认值：15000，单位：毫秒|
|scm.sdb.socketTimeout            |num    |内容服务节点与sdb的socket连接超时时长，默认值：0（不设置超时），单位：毫秒|
|scm.sdb.useNagle                |boolean|内容服务节点与sdb的连接是否使用nagle，默认值：false            |
|scm.sdb.useSSL                    |boolean|内容服务节点与sdb的连接是否使用ssl，默认值：false                |
|scm.sdb.maxConnectionNum       |num    |内容服务节点的sdb连接池的最大连接数，默认值：500|
|scm.sdb.deltaIncCount          |num    |内容服务节点的sdb连接池的每次增长的连接数，默认值：10|
|scm.sdb.maxIdleNum             |num    |内容服务节点的sdb连接池的最大空闲连接数，默认值：10|
|scm.sdb.keepAliveTime          |num    |内容服务节点的sdb连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|
|scm.sdb.recheckCyclePeriod     |num    |内容服务节点的sdb连接池定时清除连接的周期，默认值：30000，单位：毫秒|
|scm.sdb.validateConnection     |boolean|内容服务节点的sdb连接池是否开启出池检查，默认值：true|
|scm.zookeeper.urls|str|内容服务节点的zookeeper服务地址(ip1:host1,ip2:host2)|
|scm.zookeeper.cleanJobPeriod|num|内容服务节点清理zookeeper残留节点的任务周期，默认值：120000 (2min)，单位：毫秒|
|scm.zookeeper.cleanJobResidualTime|num|内容服务节点将存在超过多久的zookeeper节点作为残留节点，默认值：180000 (3min)，单位：毫秒|
|scm.zookeeper.clenaJobChildThreshold|num|内容服务节点zookeeper节点的子节点阈值，子节点数量超过阈值，清理残留子节点，默认值：1000|
|scm.zookeeper.clenaJobCountThreshold|num|内容服务节点清理任务计数阈值，任务执行次数达到阈值，清理全部残留节点，默认值：720| 
|scm.jvm.options                |str    |配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.dir.cache.enable           |boolean|内容服务节点是否开启目录缓存,默认值为：true|
|scm.dir.cache.maxSize          |num    |内容服务节点目录最大缓存记录数,默认值为：10000|
|scm.cephs3.connectionDecider.mode    |str  |配置cephs3站点主备策略，可选值：<br>auto，根据主备连接情况自动决策（默认）<br> primary_only，强制连主<br>standby_only，强制连备    |
|scm.cephs3.detectInterval            |num  |内容服务节点探测宕机的cephs3是否恢复的时间间隔，默认值：5000，单位：毫秒|
|scm.cephs3.client.connTimeout        |num  |内容服务节点与cephs3建立连接的超时时长，默认值为：10000，单位：毫秒|
|scm.cephs3.client.socketTimeout      |num  |内容服务节点与cephs3的socket连接超时时长，默认值为：50000，单位：毫秒|
|scm.cephs3.client.maxErrorRetry      |num  |内容服务节点请求cephs3的失败重试次数，默认值：3|
|scm.cephs3.client.connTTL            |num  |内容服务节点的cephs3连接池内连接的过期时间，默认值：-1（不过期），单位：毫秒 |
|scm.cephs3.client.maxConns           |num  |内容服务节点允许同时与cephs3打开的最大连接数，默认值：50  |
|scm.cephs3.client.signerOverride     |str  |cephs3驱动签名配置，默认值：S3SignerType（v2版本签名），填空串表示由客户端自动选择签名算法|
|scm.cephs3.detectClient.connTimeout  |num  |内容服务节点探测宕机的cephs3是否恢复时，建立连接的超时时长，默认值：5000，单位：毫秒|
|scm.cephs3.detectClient.socketTimeout|num  |内容服务节点探测宕机的cephs3是否恢复时，socket的连接超时时长，默认值：5000，单位：毫秒
|scm.cephs3.detectClient.maxErrorRetry|num  |内容服务节点探测宕机的cephs3是否恢复时，请求cephs3的失败重试次数，默认值：1|
|scm.cephs3.detectClient.connTTL            |num  |内容服务节点探测宕机的cephs3是否恢复时，cephs3连接池内连接的过期时间，默认值：-1（不过期），单位：毫秒 |
|scm.cephs3.detectClient.maxConns     |num  |内容服务节点探测宕机的cephs3是否恢复时，允许同时与cephs3打开的最大连接数，默认值：1|
|scm.cephs3.detectClient.signerOverride|str  |内容服务节点探测宕机的cephs3是否恢复时的签名配置，默认值： S3SignerType（v2版本签名），填空串表示由客户端自动选择签名算法|

 > **Note:**
 >
 > * 节点配置文件路径：\<内容服务安装目录\>/conf/contentserver/\<节点端口号\>/application.properties
 >
 > * scm.audit.user.xxx  xxx 表示用户的用户名，该配置项设置具体用户审计操作类型
 >
 > * scm.audit.userType.xxx  xxx 表示用户类型，该配置项设置用户类型审计操作类型，支持值详见[审计用户类型掩码列表][audit_log]
 >
 > * 审计日志多种方式配置详见[审计配置][audit_log]
 >

[public_config]:Maintainance/Node_Config/Readme.md
[audit_log]:Maintainance/Diaglog/audit_log.md





     
