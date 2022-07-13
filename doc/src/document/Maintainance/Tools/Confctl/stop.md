stop 子命令提供配置服务节点的停止功能。

##子命令选项##

|选项      |缩写  |描述                    |
|----------|------|------------------------|
|--type    |-t    |指定节点类型，可选值：all、service-center、gateway、auth-server、admin-server|
|--port    |-p    |指定特定端口，停止该节点|
|--force   |-f    |强制停止节点            |


###示例###

1. 停止端口号为 8190 的配置服务节点

  ```lang-javascript
  $ confctl.sh stop -p 8190
  ```

2. 停止所有节点

  ```lang-javascript
  $ confctl.sh stop -t all
  ```
3.  强制停止所有节点

  ```lang-javascript
  $ confctl.sh stop -t all -f
  ```
