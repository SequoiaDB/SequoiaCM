subscribe 子命令提供订阅配置服务事件的功能。

##子命令选项##

|选项       |缩写  |描述                                                            |是否必填|
|-----------|------|----------------------------------------------------------------|--------|
|--config   |-c    |需要订阅的事件的配置名称，可选值：workspace（workspace配置变更的事件通知）、meta_data(元数据模型变更的事件通知)、site(站点变更的事件通知)、node(节点变更的事件通知) |是      |
|--service  |-s    |订阅者自身的服务名，如：schedule-server、rootSite               |是      |
|--url      |-u    |配置服务节点的url，如：config-hostname:8190                     |是      |   

###示例###

为 rootsite 服务订阅 workspace 配置的变更事件通知

   ```lang-javascript
   $  confadmin.sh subscribe --config workspace --service rootsite --url localhost:8190
   ```