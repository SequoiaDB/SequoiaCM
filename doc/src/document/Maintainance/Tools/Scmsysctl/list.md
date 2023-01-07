list 子命令提供查询和显示本机所有服务节点的功能。

##子命令选项##
|选项   |缩写 |描述                                                       |
|-------|-----|-----------------------------------------------------------|
|--type    |-t   |启动指定类型的节点，可选值:all、content-server、service-center、gateway、auth-server、service-trace、admin-server、config-server、schedule-server、fulltext-server、mq-server、s3-server、om-server|
|--mode |-m   |查看本地所有节点:'local'，查看运行中的节点:'run',默认:'run'|


###示例###
1.  查看本机运行中服务的节点

  ```lang-javascript
  $ scmsysctl.sh list
  ```

2.  查看本机所有服务的节点

  ```lang-javascript
  $ scmsysctl.sh list -m local
  ```

3.  查看本机运行中网关服务的节点

  ```lang-javascript
  $ scmsysctl.sh list
  ```
