listws 子命令提供查询和显示工作区的功能。

##子命令选项##

|选项      |缩写 |描述                                              |是否必填|
|-----------|----|--------------------------------------------------|--------|
|--name     |-n  |指定查询的workspace名字，默认查询所有workspace    |否      |
|--mdsurl   |    |主站点的数据源地址，格式为:'host1:port,host2:port'|否      |
|--mdsuser  |    |主站点元数据存储服务的用户名，不指定则用户名为空  |否      |
|--mdspasswd|    |主站点元数据存储服务的密码文件绝对路径，不指定则密码为空      |否      |

> **Note:**
>
> * 参数 --mdsurl、--mdsuser、--mdspasswd 用于指定主站点元数据存储服务 SequoiaDB 的地址和用户名、密码。以便工具获取系统的元数据信息
>
> * 当本机存在 ContentServer 节点时，可以不填写参数 --mdsurl、--mdsuser、--mdspasswd，工具自动从配置文件conf/content-server/<节点目录>/application.properties中读取主站点中 SequoiaDB 服务的地址和用户名、密码

###示例###

1. 查询名字为ws的工作区

   ```lang-javascript
   $  scmadmin.sh listws --name ws
   ```
2. 查询所有工作区

   ```lang-javascript
   $  scmadmin.sh listws
   ```