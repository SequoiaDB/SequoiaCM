S3 安装包下的 deploy.py 提供 S3 服务的部署功能，deploy.py 本质上是通过 S3 管理工具、S3 节点控制工具实现的一个部署脚本，用户只需要配置相关的 json 文件，就可以通过该脚本搭建起 S3 服务。搭建 S3 服务前，需要先保证 Cloud 、Config 服务已搭建完成，并正常运行。

##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--conf    |-c         |指定部署配置文件，默认为本脚本所在路径的 deploy.json 文件|
|--bin     |-b         |指定 S3 服务工具目录，默认为本脚本所在路径的 bin 目录|
|--start   |-s         |部署完成后启动 S3 服务节点|
|--dryrun  |           |仅打印脚本执行的命令，用于实际执行前的确认及核对|

##配置文件##
如下是一个完整的配置文件示例：

```lang-javascript
{
  "s3-server": [
    {
      "hostname": "localhost",
      "server.port": "16000",
      "spring.application.name": "rootSite-s3",
      "scm.content-module.site": "rootSite",
      "scm.rootsite.meta.url": "localhost:11810",
      "scm.rootsite.meta.user": "sdbadmin",
      "scm.rootsite.meta.password": "/opt/scm4/sequoiacm/secret/metasource.pwd",
      "scm.zookeeper.urls": "localhost:2981",
      "eureka.client.prefer-same-zone-eureka": "true",
      "eureka.client.availability-zones.DefaultRegion": "zone1",
      "eureka.client.region": "DefaultRegion",
      "eureka.instance.metadata-map.zone": "zone1",
      "eureka.client.service-url.zone1": "http://192.168.16.69:8801/eureka/"
    }
  ],
  "audit": {
    "auditurl": "localhost:11810",
    "audituser": "sdbadmin",
    "auditpassword": "/opt/sequoiacm/secret/auditsource.pwd"
  }
}
```

>  **Note:**
>
>  * 整个配置文件是一个 JSON 对象，通过该对象的 s3-server 字段配置 Config 服务节点，s3-server 字段的值是一个 JSON 数组，表示一组服务实例的配置，hostname 字段表示部署到哪台主机，其它为节点配置，所有支持的配置及含义可以参考 [s3 服务节点配置][config]
>
>  * deploy.py 目前只支持部署本机站点，所以 deploy.py 将会跳过 hostname 为非本机的节点配置。用户可以规划好配置文件后，拷贝到各个需要部署节点的机器上，分别执行 deploy.py。

[config]:Maintainance/Node_Config/config.md