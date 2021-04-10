createnode 子命令提供创建消息队列服务节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：mq-server                          |是      |
|           |-D   |节点配置参数，如指定节点端口号：-Dserver.port=8180       |是|



>  **Note:**
>
>  * 消息队列服务所支持的节点配置参数请查看 [消息队列服务节点章节][message_queue_config]

###示例###

创建消息队列服务节点

   ```lang-javascript
   $  mqadmin.sh createnode --type mq-server -Dserver.port=8610 -Deureka.client.region=DefaultRegion -Deureka.instance.metadata-map.zone=zone1 -Deureka.client.availability-zones.DefaultRegion=zone1 -Deureka.client.service-url.zone1=http://192.168.31.14:8800/eureka/ -Dscm.zookeeper.urls=192.168.31.14:2181 -Dscm.store.sequoiadb.urls=localhost:11810 -Dscm.store.sequoiadb.username=root -Dscm.store.sequoiadb.password=/home/scm/sdb.passwd
   ```

[message_queue_config]:Maintainance/Node_Config/message_queue.md