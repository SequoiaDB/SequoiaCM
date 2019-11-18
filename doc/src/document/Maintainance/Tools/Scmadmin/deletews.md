deletews 子命令提供删除工作区的功能。

##子命令选项##

|选项       |缩写 |描述                                              |是否必填|
|-----------|----|--------------------------------------------------|--------|
|--name     |-n  |指定删除的workspace名字                             |是      |
|--force    |-f  |指定是否强制删除	                                    |否      |
|--url      |    |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名（小写）|是      |
|--user     |    |管理员用户名                                       |是      |
|--password |    |管理员密码                                         |是      |

> **Note:**
>
> * 参数 --url、--user、--password 用于指定主站点元数据存储服务 SequoiaDB 的地址和管理员用户名、管理员密码。
>

###示例###

1. 删除名字为ws的工作区

   ```lang-javascript
   $  scmadmin.sh deletews --name ws --url localhost:8080/rootsite --user admin --password admin
   ```
2. 强制删除名字为ws工作区

   ```lang-javascript
   $  scmadmin.sh deletews --name ws -f --url localhost:8080/rootsite --user admin --password admin
   ```