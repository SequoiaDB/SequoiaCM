start 子命令提供调度服务节点的启动功能。

##子命令选项##

|选项      |缩写 |描述                                     |
|----------|-----|-----------------------------------------|
|--type     |-t   |启动指定类型的节点，可选值：schedule-server、all|                             |
|--port    |-p   |指定特定端口，启动该节点                 |


###示例###

1. 启动端口号为 8180 的调度服务节点

  ```lang-javascript
  $ schctl.sh start -p 8180
  ```

2. 启动所有节点

  ```lang-javascript
  $ schctl.sh start -t all
  ```

>  **Note:**
> 
>  * 节点配置文件的 scm.jvm.options 配置项可以配置节点启动时的额外 JVM 参数
> 
>  * 节点配置文件位于：\<Schedule-Server安装目录\>/conf/schedule-server/\<节点端口号\>/application.properties