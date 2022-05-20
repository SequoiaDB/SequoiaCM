

公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项                     |类型   |说明                                    |
|---------------------------|-------|----------------------------------------|
|server.port|num|配置服务节点端口号|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.store.sequoiadb.urls         |str    |配置服务节点的元数据服务地址（sequoiadb的协调节点地址列表：ip1:port2,ip2:port2）|
|scm.store.sequoiadb.username         |str    |配置服务节点的元数据服务用户名（sequoiadb的用户名），默认用户名为空|
|scm.store.sequoiadb.password     |str    |配置服务节点的元数据服务密码文件路径（sequoiadb的密码），默认密码为空|
|scm.store.sequoiadb.connectTimeout     |num    |配置服务节点与sdb建立连接的超时时长，默认值：10000，单位：毫秒|
|scm.store.sequoiadb.maxAutoConnectRetryTime|num    |配置服务节点与sdb建立连接的重试时长，默认值：15000，单位：毫秒|
|scm.store.sequoiadb.socketTimeout            |num    |配置服务节点与sdb的socket连接超时时长，默认值：0（不设置超时），单位：毫秒|
|scm.store.sequoiadb.useNagle                |boolean|配置服务节点与sdb的连接是否使用nagle，默认值：false            |
|scm.store.sequoiadb.useSSL                    |boolean|配置服务节点与sdb的连接是否使用ssl，默认值：false                |
|scm.store.sequoiadb.maxConnectionNum       |num    |配置服务节点的sdb连接池的最大连接数，默认值：500|
|scm.store.sequoiadb.deltaIncCount          |num    |配置服务节点的sdb连接池的每次增长的连接数，默认值：10|
|scm.store.sequoiadb.maxIdleNum             |num    |配置服务节点的sdb连接池的最大空闲连接数，默认值：10|
|scm.store.sequoiadb.keepAliveTime          |num    |配置服务节点的sdb连接池保留空闲连接的时长，默认值：60000（不清除空闲连接），单位：毫秒|
|scm.store.sequoiadb.recheckCyclePeriod     |num    |配置服务节点的sdb连接池定时清除连接的周期，默认值：30000，单位：毫秒|
|scm.store.sequoiadb.validateConnection     |boolean|配置服务节点的sdb连接池是否开启出池检查，默认值：true|
|scm.zookeeper.urls|str|配置服务节点的zookeeper服务地址(ip1:host1,ip2:host2)|
|scm.zookeeper.cleanJobPeriod|num|配置服务节点清理zookeeper无效节点的周期，默认值：43200000 (12h)，单位：毫秒|
|scm.zookeeper.cleanJobResidualTime|num|配置服务节点将清理残留多久的zookeeper节点，默认值：86400000 (24h)，单位：毫秒|
|scm.zookeeper.acl.enabled     | boolean  | 是否开启 ZooKeeper ACL 权限控制，默认值：false。详情请见：[ZooKeeper 安全性配置][zookeeper_sercurity]|
|scm.zookeeper.acl.id          | str   | 授权对象，填写用户名密码串（username:password）的加密文件路径|

> **Note:**
>
> * 节点配置文件路径：\<config-server安装目录\>/conf/config-server/\<节点端口号\>/application.properties

[public_config]:Maintainance/Node_Config/Readme.md
[zookeeper_sercurity]:Maintainance/Security/Security_Config/zookeeper.md