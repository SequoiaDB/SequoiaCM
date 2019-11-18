stop 子命令提供 Cloud 服务节点的停止功能。

##子命令选项##

|选项      |缩写  |描述                    |
|----------|------|------------------------|
|--type    |-t    |指定节点类型，可选值： all、service-center、gateway、auth-server、service-trace、admin-server|
|--port    |-p    |指定特定端口，停止该节点|
|--force   |-f    |强制停止节点            |


###示例###

1. 停止端口号为 8190 的 Cloud 服务节点

  ```lang-javascript
  $ scmcloudctl.sh stop -p 8190
  ```

  
2. 停止所有 Gateway 服务节点

  ```lang-javascript
  $ scmcloudctl.sh stop -t gateway
  ```

3. 停止所有节点

  ```lang-javascript
  $ scmcloudctl.sh stop -t all
  ```
4. 强制停止所有节点

  ```lang-javascript
  $ scmcloudctl.sh stop -t all -f
  ```
