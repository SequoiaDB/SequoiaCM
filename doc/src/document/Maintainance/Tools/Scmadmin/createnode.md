createnode 子命令提供创建 ContentServer 节点的功能。

##子命令选项##

|选项       |缩写 |描述                                                    |是否必填|
|-----------|-----|--------------------------------------------------------|--------|
|--name     |-n   |节点的名字                                              |是      |
|--serverurl|-s   |节点的url：host:port                                    |是      |
|--sitename |     |节点所属的站点                                          |是      |
|-D\<key>=\<value> |     |节点属性,eg:-Dkey1=value1 -Dkey2=value2          |否      |
|--adurl    |     |审计日志入库地址，Sequoiadb 协调节点地址，如：sdb1:1180,sdb2:1180  |是      |
|--aduser   |     |审计日志入库用户名，Sequoiadb 用户名                    |是      |
|--adpasswd |     |审计日志入库密码，Sequoiadb 的密码文件绝对路径          |是      |
|--gateway  |     |网关地址，格式为:'host1:port,host2:port'                |是      |
|--user     |     |管理员用户名                                            |是      |
|--passwd   |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--passwd-file|   |管理员密码文件，与 passwd 互斥                          |否      |
|--mdsurl   |     |主站点元数据存储服务地址，格式为:'host1:port,host2:port'|否      |
|--mdsuser  |     |主站点元数据存储服务的用户名，不指定则用户名为空        |否      |
|--mdspasswd|     |主站点元数据存储服务的密码文件绝对路径，不指定则密码为空|否      |

>  **Note:**
>
>  * 密码文件的生成参考本章节的 [encrypt 命令][encrypt_tool]
>
>  * 3.0版本中只能在当前机器上创建节点，因此 --serverurl 参数指定的 host 只能是本机的 host。
>
>  * 参数 --mdsurl、--mdsuser、--mdspasswd 用于指定主站点元数据存储服务 SequoiaDB 的地址和用户名、密码。以便工具获取系统的元数据信息
>
>  * 参数 --passwd、--passwd-file 两者填写其一
>
>  * 需在config节点和gateway节点启动后，进行内容节点创建
>
>  * 当在本机成功创建节点后，会在生成相应的节点配置文件（conf/content-server/节点目录/application.properties）。以后所有需要指定 mdsurl，mdsuser，mdspasswd的命令均可不指定。
>
>  * -D 参数可以指定创建节点时节点配置，具体支持配置项可以参考 [ContentServer节点配置章节]
[contentserver_config]
> 
>  * 3.0.0版本中 adurl 审计信息数据存储服务仅支持设置为元数据存储服务地址 ，同样的 ，aduser 、adpasswd 参数需要设置为元数据存储服务的用户名与密码文件绝对路径
>

###示例###

在本机，创建主站点的一个服务节点

```lang-javascript
   $  scmadmin.sh createnode --sitename rootSite --name rootSiteNode --serverurl server2:15000 --mdsurl metaServer1:11810 --mdsuser sdbadmin --mdspasswd /opt/sequoiacm/secret/metasource.pwd --gateway server2:8080 --user admin --passwd admin -Deureka.instance.metadata-map.zone=zone1 -Deureka.client.region=DefaultRegion -Deureka.client.availability-zones.DefaultRegion=zone1 -Deureka.client.service-url.zone1=http://localhost:8800/eureka/ -Dscm.zookeeper.urls=localhost:2181 
```
>  **Note:**
>
>  * 本机 host 为 server2
>
>  * 主站点名为 rootSite，新增的节点名为 rootSiteNode，新增加节点的 host 为 server2，服务端口为15000
>  * 网关地址 gateway为 server2:8080，管理员用户名 user 为 admin，命令行提示输入管理员密码
>
>  * 主站点元数据存储服务地址 mdsurl 为 metaServer1:11810，mdsuser 为 sdbadmin，mdspasswd 为 /opt/sequoiacm/secret/metasource.pwd



[contentserver_config]:Maintainance/Node_Config/contentserver.md
[encrypt_tool]:Maintainance/Tools/Scmadmin/encrypt.md