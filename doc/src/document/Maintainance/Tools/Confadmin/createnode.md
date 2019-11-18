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
   $  confadmin.sh createnode --type config-server -Dserver.port=8190 
   ```

[config]:Maintainance/Node_Config/config.md