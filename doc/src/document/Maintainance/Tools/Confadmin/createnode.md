createnode 子命令提供创建配置服务节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：config-server                        |是      |
|           |-D    |节点配置参数，如指定节点端口号：-Dserver.port=8190       |是|

>  **Note:**
>
>  * 配置服务所支持的节点参数请查看 [配置服务节点章节][config]

###示例###

创建配置服务节点

   ```lang-javascript
   $  confadmin.sh createnode --type config-server -Dserver.port=8190 -Deureka.instance.metadata-map.zone=zone1 -Deureka.client.region=DefaultRegion -Deureka.client.availability-zones.DefaultRegion=zone1 -Deureka.client.service-url.zone1=http://localhost:8800/eureka/ -Dscm.zookeeper.urls=localhost:2181 -Dscm.store.sequoiadb.urls=localhost:11810 -Dscm.store.sequoiadb.username=sdbadmin -Dscm.store.sequoiadb.password=/opt/sequoiacm/secret/metasource.pwd
   ```

[config]:Maintainance/Node_Config/config.md