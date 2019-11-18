Helm 部署 SequoiaCM 的配置项描述如下

|配置项                     |类型   |说明                                    |
|---------------------------|-------|----------------------------------------|
|metasource.url|str|作为 SequoiaCM 元数据服务的 SequoiaDB 地址|
|metasource.user |str    |作为 SequoiaCM 元数据服务的 SequoiaDB 用户名|
|metasource.password |str    |作为 SequoiaCM 元数据服务的 SequoiaDB 密码|
|metasource.domain |str    |作为 SequoiaCM 元数据服务的 SequoiaDB 数据域|
|zookeeper.url |str    |zookeeper 地址|
|sites.rootSite.datasourceType |str    |主站点数据源类型|
|sites.rootSite.datasourceUrl |str    |主站点数据源地址|
|sites.rootSite.datasourceUser |str    |主站点数据源用户名|
|sites.rootSite.datasourceType |str    |主站点数据源类型|
|sites.rootSite.datasourcePassword |str    |主站点数据源密码|
|sites.rootSite.datasourceConf |str    |主站点数据源配置|
|zones.zone1.nodeAffinity |str    |zone1 下服务的调度规则|
|zones.zone1.gatewayNodePort |int    |zone1 下网关服务的 NodePort 端口|
|zones.zone1.serviceCenterNodePort |int    |zone1 下注册中心服务的 NodePort 端口|
|zones.zone1.replicaCounts.serviceCenter |int    |zone1 下注册中心服务的副本数量|
|zones.zone1.replicaCounts.gateway |int    |zone1 下网关服务的副本数量|
|zones.zone1.replicaCounts.adminServer |int    |zone1 下 Admin 服务的副本数量|
|zones.zone1.replicaCounts.authServer |int    |zone1下权限服务的副本数量|
|zones.zone1.replicaCounts.configServer |int    |zone1下配置服务的副本数量|
|zones.zone1.replicaCounts.scheduleServer |int    |zone1下调度服务的副本数量|
|zones.zone1.replicaCounts.rootSite |int    |zone1下主站点服务的副本数量|
|zones.zone1.replicaCounts.serviceCenter |int    |zone1下注册中心服务的副本数量|
|image.pullPolicy|str|SequoiaCM 镜像拉取策略|
|image.repository|str|SequoiaCM 镜像仓库地址|
|image.tag|str|SequoiaCM 镜像标签|

>  **Note：**
> 
>  * sites 配置项允许配置多个 site，格式及配置参考 rootSite
>
>  * zones 配置项允许配置多个 zone，格式及配置参考 zone1