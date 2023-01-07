start 子命令提供所有服务节点的启动功能。

##子命令选项##

|选项      |缩写 |描述                                     |
|----------|-----|-----------------------------------------|
|--type    |-t   |启动指定类型的节点，可选值:all、content-server、service-center、gateway、auth-server、service-trace、admin-server、config-server、schedule-server、fulltext-server、mq-server、s3-server、om-server|
|--timeout |     |指定超时时间，在规定时间内节点未正常运行，判定启动失败，单位：秒，默认值：200s|


###示例###
  
1. 启动所有 Gateway 服务节点

  ```lang-javascript
  $ scmsysctl.sh start -t gateway
  ```

2. 启动所有 Cloud 服务节点

  ```lang-javascript
  $ scmsysctl.sh start -t all
  ```

