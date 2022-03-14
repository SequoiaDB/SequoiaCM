
om-server 配置

|配置项                     |类型   |说明                                    |
|---------------------------|-------|----------------------------------------|
|server.port|num|配置服务节点端口号|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.omserver.gateway         |str    |scm网关服务地址列表，如：host1:8080,host2:8080|
|scm.omserver.region         |str    |本节点所在region|
|scm.omserver.zone     |str    |本节点所在zone|
|scm.omserver.sessionKeepAliveTIme     |num    |会话最大闲置间隔时间（秒），默认值900|
|scm.omserver.onlyConnectLocalRegionServer|boolean    |只连接与web服务同一个region的scm服务，默认值false|
|scm.omserver.readTimeOut            |num    |WEB服务节点与网关通信的读超时（毫秒），默认值20000|
|scm.omserver.cacheRefreshInterval |num |配置缓存的刷新时间（秒），默认值120|
|scm.omserver.healthCheckInterval |num |配置节点健康状态的检查频率（秒），默认值10|
|scm.omserver.instanceUpdateInterval |num |配置节点列表的更新频率（秒），默认值30|




> **Note:**
>
> * 节点配置文件路径：\<om-server安装目录\>/conf/om-server/\<节点端口号\>/application.properties


