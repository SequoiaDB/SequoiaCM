cloud 安装包下的 deploy.py 提供 Cloud 服务的部署功能，deploy.py 本质上是通过 Cloud 管理工具、Cloud 节点控制工具实现的一个部署脚本，用户只需要配置一份 deploy.json 文件，就可以通过该脚本搭建起 Cloud 服务。

##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--conf    |-c         |指定部署配置文件，默认为本脚本所在路径的 deploy.json 文件|
|--bin     |-b         |指定 Cloud 工具目录，默认为本脚本所在路径的 bin 目录|
|--start   |-s         |部署完成后启动 Cloud 节点|
|--cleansystable|      |清除 Cloud 服务相关的系统表，二次部署存在残留系统表时可以指定该参数|
|--dryrun  |           |仅打印脚本执行的命令，用于实际执行前的确认及核对|

##配置文件##
如下是一个完整的配置文件示例：

```lang-javascript
{
    "gateway": [
        {
            "hostname":"localhost",
            "server.port": "8080",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.register-with-eureka": "true",
            "eureka.client.fetch-registry": "true",
            "eureka.client.prefer-same-zone-eureka": "true",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "spring.zipkin.enabled": "false",
            "spring.zipkin.base-url": "http://localhost:8890",
            "scm.jvm.options":"''"
        }
    ],
    "serviceCenter": [
        {
            "hostname":"localhost",
            "server.port": "8800",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.register-with-eureka": "true",
            "eureka.client.fetch-registry": "true",
            "eureka.client.prefer-same-zone-eureka": "true",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "scm.jvm.options":"''"
        }
    ],
    "authServer": [
        {
            "hostname":"localhost",
            "server.port": "8810",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.register-with-eureka": "true",
            "eureka.client.fetch-registry": "true",
            "eureka.client.prefer-same-zone-eureka": "true",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "spring.zipkin.enabled": "false",
            "spring.zipkin.base-url": "http://localhost:8890",
            "scm.store.sequoiadb.urls": "localhost:11810",
            "scm.store.sequoiadb.username": "sdbadmin",
            "scm.store.sequoiadb.password": "/home/linyoubin/scm/scm-cloud/sdb.passwd",
            "scm.auth.token.enabled": "false",
            "scm.auth.token.allowAnyValue": "false",
            "scm.auth.token.tokenValue": "token123",
            "spring.ldap.urls": "",
            "spring.ldap.username": "", 
            "spring.ldap.password": "",
            "spring.ldap.base": "",
            "spring.ldap.usernameAttribute": "uid",
            "scm.audit.mask":"ALL",
            "scm.audit.userMask":"TOKEN",
            "scm.jvm.options":"''"
        }
    ],
    "serviceTrace": [
        {
            "hostname":"localhost",
            "server.port": "8890",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.register-with-eureka": "true",
            "eureka.client.fetch-registry": "true",
            "eureka.client.prefer-same-zone-eureka": "true",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "scm.jvm.options":"''"
        }
    ],
    "adminServer": [
        {
            "hostname":"localhost",
            "server.port": "8900",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.register-with-eureka": "true",
            "eureka.client.fetch-registry": "true",
            "eureka.client.prefer-same-zone-eureka": "true",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "scm.jvm.options":"''",
            "spring.zipkin.enabled": "true",
            "spring.zipkin.base-url": "http://localhost:8890",
            "scm.store.sequoiadb.urls": "localhost:11810",
            "scm.store.sequoiadb.username": "sdbadmin",
            "scm.store.sequoiadb.password": "/home/linyoubin/scm/scm-cloud/sdb.passwd",
            "scm.statistics.job.firstTime": "00:00:00",
            "scm.statistics.job.period": "1d"
        }
    ],
    "audit": {
        "auditurl":"localhost:11810",
        "audituser":"sdbadmin",
        "auditpassword":"/home/linyoubin/scm/scm-cloud/sdb.passwd"
   }
}
```
>  **Note:**
> 
>  * 整个配置文件是一个 JSON 对象，通过该对象的 gateway、serviceCenter、authServer、serviceTrace、adminServer 等字段指定不同 cloud 服务的节点配置，user 字段用于指定部署 cloud 服务的同时新建一个用户，audit 字段用于指定所有 cloud 服务的审计日志配置。
>
>  * gateway、serviceCenter、authServer、serviceTrace、adminServer 字段用于描述指定服务实例的配置，这些字段的值是一个 JSON 数组，表示一组服务实例的配置，hostname 字段表示部署到哪台主机，其它为节点配置，所有支持的配置及含义可以参考 [cloud 节点配置][cloud_config]
>
>  * deploy.py 目前只支持部署本机站点，所以 deploy.py 将会跳过 hostname 为非本机的节点配置。用户可以规划好配置文件后，拷贝到各个需要部署节点的机器上，分别执行 deploy.py。
>
>  * audit 字段表示 cloud 服务节点的审计配置，其中 auditurl 表示审计日志的入库地址（SequoiaDB），audituser 表示用户名，auditpassword 表示密码文件（密码文件通过 [encrypt 命令][encrypt_tool]生成）
>

[cloud_config]:Maintainance/Node_Config/cloud.md
[encrypt_tool]:Maintainance/Tools/Scmadmin/encrypt.md
  




