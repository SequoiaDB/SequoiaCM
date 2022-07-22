createnode 子命令提供创建 S3 服务节点的功能。

## 子命令选项 ##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：s3-server                        |是      |
|           |-D   |节点配置参数，如指定节点端口号：-Dserver.port=8180       |是|



>  **Note:**
>
>  * S3 服务所支持的节点配置参数请查看 [s3服务节点章节][s3_config]

### 示例 ###

创建 s3 服务节点

   ```lang-javascript
   $ s3admin.sh createnode --type s3-server -Dserver.port=16100 -Deureka.client.region=DefaultRegion -Dscm.content-module.site=rootsite -Dspring.application.name=rootsite-s3 -Deureka.client.service-url.zone1=http://service-center-host:8801/eureka/ -Deureka.instance.metadata-map.zone=zone1  -Deureka.client.availability-zones.DefaultRegion=zone1 -Dscm.zookeeper.urls=zookeeper:2981 -Dscm.rootsite.meta.url=metasource-sdb:11810 -Dscm.rootsite.meta.user=sdbadmin -Dscm.rootsite.meta.password=/opt/scm4/sequoiacm/secret/metasource.pwd  --adurl audit-sdb:11810 --aduser sdbadmin --adpasswd /opt/scm4/sequoiacm/secret/auditsource.pwd
   ```

[s3_config]:Maintainance/Node_Config/s3.md