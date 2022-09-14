listsite 子命令提供查看站点的功能。

##子命令选项##
|选项       |缩写  |描述                                                            |是否必填|
|-----------|------|----------------------------------------------------------------|--------|
|--name     |-n    |指定查询的站点名字，默认查询所有站点                            |否      |
|--mdsurl   |      |主站点元数据存储服务的地址，格式为:'server1:11810,server2:11810'|否      |
|--mdsuser  |      |主站点元数据存储服务的用户名，不指定则用户名为空                |否      |
|--mdspasswd|      |主站点元数据存储服务的密码文件绝对路径，不指定则密码为空        |否      |

> **Note:**
>
> * 参数 --mdsurl、--mdsuser、--mdspasswd 用于指定主站点中元数据存储服务 SequoiaDB 的地址和用户名、密码。以便工具获取系统的元数据信息
>
> * 当本机存在 ContentServer 节点时，可以不填写参数 --mdsurl、--mdsuser、--mdspasswd，工具自动从配置文件conf/content-server/<节点目录>/application.properties中读取主站点中 SequoiaDB 服务的地址和用户名、密码

###示例###

1. 查询站点名字为 site1 的站点

   ```lang-javascript
   $  scmadmin.sh listsite --name site1 --mdsurl metaServer1:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd
   ```

>  **Note:**
>
>  * 主站点元数据存储服务地址 mdsurl 为 metaServer1:11810 ，mdsuser 为sdbadmin，mdspasswd 为 /home/scmadmin/sdb.passwd

2. 查询所有站点

   ```lang-javascript
   $  scmadmin.sh listsite
   ```

>  **Note:**
>
>  * 存在节点信息（ conf/content-server/<节点目录>/application.properties ）时，自动从配置文件中获取 mdsurl、mdsuser、mdspasswd，不需要在命令行中指定这三个参数。