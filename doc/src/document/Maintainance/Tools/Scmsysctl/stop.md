stop 子命令提供服务节点的停止功能。

##子命令选项##

|选项      |缩写  |描述                    |
|----------|------|------------------------|
|--type    |-t    |指定节点类型，可选值： all、content-server、service-center、gateway、auth-server、service-trace、admin-server、config-server、schedule-server、fulltext-server、mq-server、s3-server、om-server|
|--force   |-f    |强制停止节点            |
|--timeout |     |指定超时时间，在规定时间内节点未全部正常停止，判定停止失败，单位：秒，默认值：30s|


###示例###
  
1. 停止所有 Gateway 服务节点

  ```lang-javascript
  $ scmsysctl.sh stop -t gateway
  ```

2. 停止所有节点

  ```lang-javascript
  $ scmsysctl.sh stop -t all
  ```
3. 强制停止所有节点

  ```lang-javascript
  $ scmsysctl.sh stop -t all -f
  ```
