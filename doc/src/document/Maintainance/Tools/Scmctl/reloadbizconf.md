reloadbizconf 子命令提供刷新节点业务配置的功能。

##子命令选项##
|选项          |缩写 |描述                                                                                               |
|--------------|-----|---------------------------------------------------------------------------------------------------|
|--running-node|-r   |指定一个运行中的内容服务节点节点地址,格式：host:port,刷新请求将由该节点发起,默认值为 localhost:15000|
|--all         |-a   |刷新所有内容服务节点节点                                                                            |
|--site        |-s   |刷新指定站点下的内容服务节点节点                                                                    |
|--node        |-n   |刷新指定内容服务节点节点                                                                            |

###示例###
1.  刷新所有内容服务节点节点，存在一个可连接的内容服务节点节点为：server1:15000

  ```lang-javascript
  $ scmctl.sh reloadbizconf -r server1:15000 -a 
  ```

2.  刷新站点 site1 下的所有节点，存在一个可连接的内容服务节点节点为：server1:15000

  ```lang-javascript
  $ scmctl.sh reloadbizconf -r server1:15000 -s site1
  ```

3.  刷新名为 node1 的节点，存在一个可连接的内容服务节点节点为：server1:15000
  
  ```lang-javascript
  $ scmctl.sh reloadbizconf -r server1:15000 -n node1
  ```