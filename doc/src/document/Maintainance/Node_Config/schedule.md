公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明| 生效类型 |
|------|-----|-----|----------|
|scm.store.sequoiadb.urls|str|元数据服务 SequoiaDB 的协调节点服务地址。例如：192.168.20.56:11810,192.168.20.57:11810|重启生效|
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
|scm.store.sequoiadb.validateConnection|bool|出池时是否检查连接有效性, 默认值：true|重启生效|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|scm.audit.mask|str|审计日志操作配置，控制对不同操作命令记录审计日志。默认为空，不记录任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.audit.userMask|str|审计日志用户配置，默认为空，不记录任何用户的审计日志。支持的配置详见[审计用户类型掩码列表][audit_log]|在线生效|
|scm.audit.user.xxx|str|审计日志操作配置，控制具体用户对不同操作命令记录审计日志。默认为空，不记录用户名为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.audit.userType.xxx|str|审计日志操作配置，控制用户类型对不同操作命令记录审计日志。默认为空，不记录用户类型为 xxx 用户任何操作的审计日志。支持的配置详见[审计操作类型掩码列表][audit_log]|在线生效|
|scm.privilege.heartbeat.interval|num|权限版本号校验间隔时间，当版本号不一致时会刷新版本信息。默认值：10000，单位：毫秒|重启生效|
|scm.conf.version.siteHeartbeat|num|配置站点版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.version.nodeHeartbeat|num|配置节点版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.conf.version.workspaceHeartbeat|num|配置工作区版本号校验间隔时间。默认值：180000，单位：毫秒|重启生效|
|scm.zookeeper.urls|str|调度服务节点的zookeeper服务地址(ip1:host1,ip2:host2)|重启生效|
|scm.revote.initialInterval|num|节点当选主节点后初始化失败时，起始静默时间，默认值：100，单位：毫秒|重启生效|
|scm.revote.intervalMultiplier|num|节点连续当选主节点后初始化失败时，静默时间递增倍数，默认值：2|重启生效|
|scm.revote.maxInterval|num|节点当选主节点后初始化失败时，静默时间的上限，默认值：60000，单位：毫秒| 
|scm.zookeeper.acl.enabled     | boolean  | 是否开启 ZooKeeper ACL 权限控制，默认值：false。详情请见：[ZooKeeper 安全性配置][zookeeper_sercurity]|重启生效|
|scm.zookeeper.acl.id          | str   | 授权对象，填写用户名密码串（username:password）的加密文件路径|重启生效|

 > **Note:**
 >
 > * scm.audit.user.xxx  xxx 表示用户的用户名，该配置项设置具体用户审计操作类型
 >
 > * scm.audit.userType.xxx  xxx 表示用户类型，该配置项设置用户类型审计操作类型，支持值详见[审计用户类型掩码列表][audit_log]
 >
 > * 审计日志多种方式配置详见[审计配置][audit_log]
 >

[public_config]:Maintainance/Node_Config/Readme.md
[audit_log]:Maintainance/Diaglog/audit_log.md
[zookeeper_sercurity]:Maintainance/Security/Security_Config/zookeeper.md