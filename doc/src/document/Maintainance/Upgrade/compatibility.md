## 兼容性列表##

| 升级前版本 | 升级到2.* | 升级到3.0 | 升级到3.1  |
|------------|-----------|-----------|------------|
| 2.*        | ●         | ×         | ×          |
| 3.0        | ×         | ●         | ●          |
| 3.1        | ×         | ×         | ●          |

> **Note：**
>
>  * SequoiaCM 不支持版本降级。
>
>  * × 表示不支持升级
>
>  * ○ 表示仅支持集群所有服务同时离线升级
>
>  * ● 表示支持在线滚动升级，集群离线升级

## 版本配置变更 ##
### SequoiaCM version 3.2 ###
**新增 service-center 节点配置：**
<br>
3.2 版本引入了系统状态监控功能，需要依赖 service-center 存储节点列表，因此在升级到该版本前，请在 service-center 节点的配置文件中加入数据源的相关配置，具体的配置请参考 [cloud 节点配置][cloud_config]。

> **Note:**
>
> * 节点配置文件路径：\< cloud 节点安装目录\>/conf/service-center/\<节点端口号\>/application.properties


[cloud_config]:Maintainance/Node_Config/cloud.md

**新增公共节点配置：**
>  * management.port: 为支撑系统状态监控，3.2 版本的微服务节点会额外占用一个管理端口号，通过 management.port 配置项配置，不指定管理端口时，默认为 server.port + 1，除 s3-server 外的其它服务均支持将 management.port 设为与 server.port 一致。在升级前请确保各微服务的管理端口不会产生冲突。