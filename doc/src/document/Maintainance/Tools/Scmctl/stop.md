stop 子命令提供内容服务节点节点的停止功能。

##子命令选项##

|选项      |缩写  |描述                    |
|----------|------|------------------------|
|--all     |-a    |停止所有节点            |
|--port    |-p    |指定特定端口，停止该节点|
|--force   |-f    |强制停止节点            |


###示例###

1. 停止端口号为 15000 的内容服务节点节点

  ```lang-javascript
  $ scmctl.sh stop -p 15000
  ```

2. 停止所有节点

  ```lang-javascript
  $ scmctl.sh stop -a
  ```
3.  强制停止所有节点

  ```lang-javascript
  $ scmctl.sh stop -a -f
  ```