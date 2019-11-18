deletenode 子命令提供删除 ContentServer 节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                    |是否必填|
|-----------|-----|--------------------------------------------------------|--------|
|--port     |-p   |节点的端口号                                            |是      |
|--url      |     |网关地址，格式为:'host1:port,host1:port'                |是      |
|--user     |     |管理员用户名                                            |是      |
|--password |     |管理员密码                                              |是      |

>  **Note:**
>
>  * 需在config节点和gateway节点启动后，进行内容节点删除
>
>  * 删除节点，会删除日志和配置文件


###示例###

在本机，删除站点的一个内容服务节点

```lang-javascript
   $  scmadmin.sh deletenode --port 15100 --url server2:8080 --user admin --password admin
```
>  **Note:**
>
>  * 节点端口号 port 为 15100， 网关地址 url 为 server2:8080，管理员用户名 user 为 admin，管理员密码 password 为 admin



