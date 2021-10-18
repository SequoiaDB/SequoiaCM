OM服务的部署方式与其它服务节点相似，可在部署SequoiaCM集群时通过一键部署脚本快速部署，也可单独部署。

## 快速部署 ##

- 在部署工具配置文件deploy.cfg的基础服务节点配置中，增加om-server节点：

```
[servicenode]
ZoneName,  ServiceType,       HostName,       Port,  CustomNodeConf
zone1,     om-server,         scmServer,      9000,  
zone1,     service-center,    scmServer,      8800,  
zone1,     auth-server,       scmServer,      8810,  
zone1,     gateway,           scmServer,      8080,  
zone1,     config-server,     scmServer,      8190,
zone1,     schedule-server,   scmServer,      8910,
zone1,     admin-server,      scmServer,      8900, 
zone1,     mq-server,         scmServer,      8710, 
zone1,     fulltext-server,   scmServer,      8610, '{"scm.fulltext.es.urls": "http://esServer:9200", "scm.fulltext.textualParser.pic.tessdataDir": "/usr/share/tesseract-ocr/tessdata/"}'
```

- 执行部署脚本，快速部署工具的详细使用说明请见[快速部署][quick_deploy]章节。

## 单独部署 ##

- 如果部署OM服务节点的机器上没有该服务的安装包，需要首先获取到该服务安装包，并解压到目标机器上:

```
$ tar -zxvf sequoiacm-om-1.0.0-release.tar.gz -C /opt/sequoiacm
```

- 编辑 OM 服务部署配置文件：

```
$ vi /opt/sequoiacm/sequoiacm-om/deploy.json
{
    "om-server": [
        {
            "hostname": "localhost",
            "server.port": "9000",
            "scm.omserver.gateway": "192.168.16.70:8080",
            "scm.omserver.readTimeout": "5000",
            "scm.omserver.onlyConnectLocalRegionServer": "false",
            "scm.omserver.sessionKeepAliveTime": "900",
            "scm.omserver.cacheRefreshInterval": "180",
            "scm.omserver.region": "DefaultRegion",
            "scm.omserver.zone": "zone1"
        }
    ]

}
```

>  **Note:**
>
>  * OM服务通过SequoiaCM驱动连接网关操作SequoiaCM集群，需要在scm.omserver.gateway中指定网关服务节点的地址，可以配置多个（使用","分隔）
> 
>  * OM服务所有支持的配置及含义可以参考 [OM服务节点配置][config]

- 执行部署命令，并启动OM节点

```
$ python /opt/sequoiacm/sequoiacm-om/deploy.py --start
```

- 假设OM服务节点部署在9000端口上，通过浏览器访问OM服务节点的地址http://ip:9000，验证是否部署成功。


[config]:Maintainance/Node_Config/om.md
[quick_deploy]:Quick_Start/deploy.md
