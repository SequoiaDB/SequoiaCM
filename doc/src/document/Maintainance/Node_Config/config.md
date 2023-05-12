

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项                     |类型   |说明                                    | 生效类型 |
|---------------------------|-------|----------------------------------------|----------|
|server.port|num|配置服务节点端口号|重启生效|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|重启生效|
|scm.store.sequoiadb.urls         |str    |配置服务节点的元数据服务地址（sequoiadb的协调节点地址列表：ip1:port2,ip2:port2）|重启生效|
|scm.store.sequoiadb.username         |str    |配置服务节点的元数据服务用户名（sequoiadb的用户名），默认用户名为空|重启生效|
|scm.store.sequoiadb.password     |str    |配置服务节点的元数据服务密码文件路径（sequoiadb的密码），默认密码为空|重启生效|
|scm.store.sequoiadb.connectTimeout     |num    |配置服务节点与sdb建立连接的超时时长，默认值：10000，单位：毫秒|重启生效|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num    |配置服务节点与sdb建立连接的重试时长，默认值：15000，单位：毫秒|重启生效|
|scm.store.sequoiadb.socketTimeout            |num    |配置服务节点与sdb的socket连接超时时长，默认值：0（不设置超时），单位：毫秒|重启生效|
|scm.store.sequoiadb.useNagle                |boolean|配置服务节点与sdb的连接是否使用nagle，默认值：false            |重启生效|
|scm.store.sequoiadb.useSSL                    |boolean|配置服务节点与sdb的连接是否使用ssl，默认值：false                |重启生效|
|scm.store.sequoiadb.maxConnectionNum       |num    |配置服务节点的sdb连接池的最大连接数，默认值：500|重启生效|
|scm.store.sequoiadb.deltaIncCount          |num    |配置服务节点的sdb连接池的每次增长的连接数，默认值：10|重启生效|
|scm.store.sequoiadb.maxIdleNum             |num    |配置服务节点的sdb连接池的最大空闲连接数，默认值：10|重启生效|
|scm.store.sequoiadb.keepAliveTime          |num    |配置服务节点的sdb连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|重启生效|
|scm.store.sequoiadb.recheckCyclePeriod     |num    |配置服务节点的sdb连接池定时清除连接的周期，默认值：30000，单位：毫秒|重启生效|
|scm.store.sequoiadb.validateConnection     |boolean|配置服务节点的sdb连接池是否开启出池检查，默认值：true|重启生效|
|scm.store.sequoiadb.location|str|元数据服务 SequoiaDB 连接池 location 配置，默认值为空|重启生效|
|scm.zookeeper.urls|str|配置服务节点的zookeeper服务地址(ip1:host1,ip2:host2)|重启生效|
|scm.zookeeper.cleanJobPeriod                |num|配置服务节点全量清理zookeeper无效节点的周期，默认值：1800000 (30分钟)，单位：毫秒                           |重启生效|
|scm.zookeeper.maxBuffer                     |num|配置服务节点全量清理zookeeper无效节点时所使用的最大buffer大小，默认使用 JVM 最大堆内存的 1/5，单位：字节       |重启生效|
|scm.zookeeper.cleanJobResidualTime          |num|配置服务节点将清理残留多久的zookeeper节点，默认值：180000 (3分钟)，单位：毫秒                              |重启生效|
|scm.zookeeper.maxCleanThreads               |num|配置服务节点清理残留的zookeeper节点所使用的最大线程数，默认值：6                                         |重启生效|
|scm.zookeeper.cleanQueueSize                |num|配置服务节点清理残留的zookeeper节点所使用的异步缓存队列的大小，默认值：10000|重启生效|
|scm.zookeeper.acl.enabled     | boolean  | 是否开启 ZooKeeper ACL 权限控制，默认值：false。详情请见：[ZooKeeper 安全性配置][zookeeper_sercurity]|重启生效|
|scm.zookeeper.acl.id          | str   | 授权对象，填写用户名密码串（username:password）的加密文件路径|重启生效|

> **Note:**
>
> * 节点配置文件路径：\<config-server安装目录\>/conf/config-server/\<节点端口号\>/application.properties

[public_config]:Maintainance/Node_Config/Readme.md
[zookeeper_sercurity]:Maintainance/Security/Security_Config/zookeeper.md