Message-Queue-Server 安装包下的 deploy.py 提供 Message-Queue-Server 服务的部署功能，deploy.py 本质上是通过 Message-Queue-Server 管理工具、Message-Queue-Server 节点控制工具实现的一个部署脚本，用户只需要配置一份 deploy.json 文件，就可以通过该脚本搭建起 Message-Queue-Server 服务。


##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--conf    |-c         |指定部署配置文件，默认为本脚本所在路径的 deploy.json 文件|
|--bin     |-b         |指定 Message-Queue-Server 工具目录，默认为本脚本所在路径的 bin 目录|
|--start   |-s         |部署完成后启动 Message-Queue-Server节点|
|--dryrun  |           |仅打印脚本执行的命令，用于实际执行前的确认及核对|

##配置文件##
如下是一个简要的配置文件示例：

```lang-javascript
{
    "mq-server": [
        {
            "hostname": "localhost",
            "server.port": "8190",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.region": "DefaultRegion",
            "eureka.client.availability-zones.DefaultRegion": "zone1",
            "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
            "scm.zookeeper.urls": "localhost:2181",
            "scm.store.sequoiadb.urls": "localhost:11810",
            "scm.store.sequoiadb.username": "sdbadmin",
            "scm.store.sequoiadb.password": "/opt/sequoiacm/secret/metasource.pwd"
        }
    ]
}
```

>  **Note:**
>
>  * 整个配置文件是一个 JSON 对象，通过该对象的 mq-server 字段配置 Message-Queue-Server 服务节点，Message-Queue-Server 字段的值是一个 JSON 数组，表示一组服务实例的配置，hostname 字段表示部署到哪台主机，其它为节点配置，所有支持的配置及含义可以参考 [Message-Queue-Server 节点配置][config]
>
>  * deploy.py 目前只支持部署本机站点，所以 deploy.py 将会跳过 hostname 为非本机的节点配置。用户可以规划好配置文件后，拷贝到各个需要部署节点的机器上，分别执行 deploy.py。

[config]:Maintainance/Node_Config/message_queue.md