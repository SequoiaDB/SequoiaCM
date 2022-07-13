createnode 子命令提供创建om服务节点的功能。

## 子命令选项 ##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--type     |-t   |节点类型，可选值：om-server                        |是      |
|           |-D   |节点配置参数，如指定节点端口号：-Dserver.port=8180       |是|



>  **Note:**
>
>  * om管理服务所支持的节点配置参数请查看 [om服务节点章节][om_config]

### 示例 ###

创建om服务节点

   ```lang-javascript
   $  omadmin.sh createnode --type om-server -Dserver.port=9000 -Dscm.omserver.gateway=192.168.16.70:8080  -Dscm.omserver.region=DefaultRegion -Dscm.omserver.zone=zone1
   ```

[om_config]:Maintainance/Node_Config/om.md