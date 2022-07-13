createnode 子命令提供创建调度服务节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：config-server                        |是      |
|           |-D   |节点配置参数，如指定节点端口号：-Dserver.port=8180       |是|
|--adurl    |     |审计日志入库地址，Sequoiadb 协调节点地址，如：sdb1:1180,sdb2:1180							|是|
|--aduser   |     |审计日志入库用户名，Sequoiadb 用户名|是|
|--adpasswd |     |审计日志入库密码，Sequoiadb 的密码文件绝对路径|是|


>  **Note:**
>
>  * 调度服务所支持的节点配置参数请查看 [调度服务节点章节][schedule_config]
>
>  * Sequoiadb 的密码文件生成请查看 [encrypt 命令][encrypt_tool]
>
>  * 3.0.0版本中 adurl 审计信息数据存储服务仅支持设置为元数据存储服务地址 ，同样的 ，aduser 、adpasswd 参数需要设置为元数据存储服务的用户名与密码文件绝对路径

###示例###

创建调度服务节点

   ```lang-javascript
   $  schadmin.sh createnode --type schedule-server --adurl localhost:11810 --aduser sdbadmin --adpasswd /opt/sequoiacm/secret/auditsource.pwd -Dserver.port=8180 -Deureka.instance.metadata-map.zone=zone1 -Deureka.client.region=DefaultRegion -Deureka.client.availability-zones.DefaultRegion=zone1 -Deureka.client.service-url.zone1=http://localhost:8800/eureka/ -Dscm.zookeeper.urls=localhost:2181 -Dscm.store.sequoiadb.urls=localhost:11810 -Dscm.store.sequoiadb.username=sdbadmin -Dscm.store.sequoiadb.password=/opt/sequoiacm/secret/metasource.pwd
   ```

[schedule_config]:Maintainance/Node_Config/schedule.md
[encrypt_tool]:Maintainance/Tools/Scmadmin/encrypt.md