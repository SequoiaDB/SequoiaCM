start 子命令提供 Cloud 服务节点的启动功能。

##子命令选项##

|选项      |缩写 |描述                                     |
|----------|-----|-----------------------------------------|
|--type    |-t   |启动指定类型的节点，可选值:all、service-center、gateway、auth-server、admin-server|
|--port    |-p   |指定特定端口，启动该节点                 |
|--timeout |     |指定超时时间，在规定时间内节点未正常运行，判定启动失败，单位：秒，默认值：50s|


###示例###

1. 启动端口号为 8190 的 Cloud 服务节点

  ```lang-javascript
  $ scmcloudctl.sh start -p 8080
  ```
  
2. 启动所有 Gateway 服务节点

  ```lang-javascript
  $ scmcloudctl.sh start -t gateway
  ```

3. 启动所有 Cloud 服务节点

  ```lang-javascript
  $ scmcloudctl.sh start -t all
  ```
  
>  **Note:**
> 
>  * 节点配置文件的 scm.jvm.options 配置项可以配置节点启动时的额外 JVM 参数
>
>  * 节点配置文件位于：\<SequoiaCM-Cloud安装目录\>/conf/\<节点类型\>/\<节点端口号\>/application.properties

