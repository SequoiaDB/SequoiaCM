deletesite 子命令提供删除 ContentServer 站点的功能。

##子命令选项##

|选项       |缩写 |描述                                                    |是否必填|
|-----------|-----|--------------------------------------------------------|--------|
|--name     |-n   |站点名                                                  |是      |
|--url      |     |网关地址，格式为:'host1:port,host1:port'                |是      |
|--user     |     |管理员用户名                                            |是      |
|--password |     |管理员密码                                              |是      |

>  **Note:**
>
>  * 需在 config 节点和 gateway 节点启动后，进行内容站点删除
>
>  * 删除分站点，不能存在属于该站点的节点，不能存在有数据源落该站点的工作区
>   
>  * 删除主站点，不能存在其他分站点，不能存在属于该站点的节点，不能存在有数据源落该站点的工作区

###示例###

在本机，删除内容服务的一个分站点

```lang-javascript
   $  scmadmin.sh deletesite --name branchSite1 --url server2:8080 --user admin --password admin
```
>  **Note:**
>
>  * 站点名 name 为 branchSite1，网关地址 url 为 server2:8080，管理员用户名 user 为 admin，管理员密码 password 为 admin



