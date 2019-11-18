start 子命令提供配置服务节点的启动功能。

##子命令选项##

|选项      |缩写 |描述                                     |
|----------|-----|-----------------------------------------|
|--type     |-t   |启动指定类型的节点，可选值：config-server、all|                             |
|--port    |-p   |指定特定端口，启动该节点                 |


###示例###

1. 启动端口号为 8190 的配置服务节点

  ```lang-javascript
  $ confctl.sh start -p 8190
  ```

2. 启动所有节点

  ```lang-javascript
  $ confctl.sh start -t all
  ```

>  **Note:**
> 
>  * 节点配置文件的 scm.jvm.options 配置项可以配置节点启动时的额外 JVM 参数
>
>  * 节点配置文件位于：\<Config-Server安装目录\>/conf/config-server/\<节点端口号\>/application.properties