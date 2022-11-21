如果新增节点涉及到新增主机，则请首先按照在集群中新增主机一节完成主机的主机名和参数配置，如果新增节点的机器上没有该服务的安装包，需要首先获取到该服务安装包，并解压到目标机器上，如 Cloud 服务：

```
$ tar -zxvf sequoiacm-cloud-1.0.0-release.tar.gz -C /opt/sequoiacm
```


> **Note:**
>
>  * 以下示例的 SequoiaCM 安装路径均为 '/opt/sequoiacm'


##增加 Cloud 服务节点##

- 编辑 Cloud 服务部署配置文件，配置需要新增的服务节点，如新增一个网关服务节点和认证服务节点：

```
$ vi /opt/sequoiacm/sequoiacm-cloud/deploy.json
{
    "gateway":[
	    {
		    "hostname":"scmServer2",
			"server.port":"8080",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://scmServer2:8800/eureka/",
			"scm.jvm.options":"''"
		}
	],
    "authServer":[
	    {
		    "hostname":"scmServer2",
			"server.port":"8810",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://scmServer2:8800/eureka/",
            "scm.store.sequoiadb.urls":"sdbServer:11810",
			"scm.store.sequoiadb.username":"sdbadmin",
            "scm.store.sequoiadb.password": "/opt/sequoiacm/sdb.passwd",
			"scm.auth.token.enabled":"true",
			"scm.auth.token.allowAnyValue":"true",
			"scm.auth.toKen.tokenValue":"token123",
			"spring.ldap.urls":"",
			"spring.ldap.username":"",
			"spring.ldap.password":"",
			"spring.ldap.base":"",
			"spring.ldap.usernameAttribute":"uid",
			"scm.audit.userType.TOKEN":"ALL",
			"scm.jvm.options":"''"
		}
	],
    "audit":{
		"auditurl":"sdbServer:11810",
		"audituser":"sdbadmin",
		"auditpassword":"/opt/sequoiacm/sdb.passwd"
	}
}
```

>  **Note:**
>
>  * 注册中心地址、元数据储存地址需要按已有环境的配置填写
>
>  * 如果需要新增节点的机器上没有密码文件，即'/opt/sequoiacm/sdb.passwd'，可以从原有环境的部署主机上拷贝获取

- 执行部署命令，并启动新增节点

```
$ python /opt/sequoiacm/sequoiacm-cloud/deploy.py --start
```

##增加核心服务节点##
 
###增加内容服务节点###

- 编辑内容服务部署配置文件，配置需要新增的内容服务节点，如在主站点新增内容服务节点：


```
$ vi /opt/sequoiacm/sequoiacm-content/deploy.json
{
    "sites": [],
	"nodes":[
		{
			"node":[
				{
					"name":"rootNode",
					"url":"scmServer1:15001",
					"siteName":"rootSite",
					"customProperties":{
					    "eureka.instance.metadata-map.zone": "zone1",
                        "eureka.client.region": "beijing",
                        "eureka.client.availability-zones.beijing": "zone1",
                        "eureka.client.service-url.zone1": "http://scmServer2:8800/eureka/",
						"scm.zookeeper.urls":"zookeeperServer:2181",
						"scm.jvm.options":"''",
						"scm.audit.userType.TOKEN":"ALL",
                        "scm.sdb.maxAutoConnectRetryTime":"1000"
					}
				}
			]
		}
	],
	"audit": {
		"auditurl":"sdbServer",
		"audituser":"sdbadmin",
		"auditpassword":"/opt/sequoiacm/sdb.passwd"
    },
    "gateway":{
        "url":"gatewayUrl:port",
        "user":"admin",
        "password":"admin"
    }

}
```


>  **Note:**
>
>  * 注册中心地址、元数据储存地址需要按已有环境的配置填写
>
>  * 如果需要新增节点的机器上没有密码文件，即'/opt/sequoiacm/sdb.passwd'，可以从原有环境的部署主机上拷贝获取

- 执行部署命令，并启动新增节点

```
$ python /opt/sequoiacm/sequoiacm-content/deploy.py --createnode --start
```

###增加配置服务节点###

- 编辑配置服务部署配置文件，配置需要新增的配置服务节点，如新增一个配置服务节点：

```
$ vi /opt/sequoiacm/sequoiacm-config/deploy.json
{
    "config-server":[
	    {
		    "hostname":"scmServer1",
			"server.port":"8190",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://scmServer2:8800/eureka/",
			"scm.zookeeper.urls":"zookeeperServer:2181",
			"scm.store.sequoiadb.urls":"sdbServer:11810",
            "scm.store.sequoiadb.username": "sdbadmin",
            "scm.store.sequoiadb.password": "/opt/sequoiacm/sdb.passwd"
		}
	]
}
```

>  **Note:**
>
>  * 注册中心地址、元数据储存地址需要按已有环境的配置填写
>
>  * 如果需要新增节点的机器上没有密码文件，即'/opt/sequoiacm/sdb.passwd'，可以从原有环境的部署主机上拷贝获取

- 部署并启动新增配置服务节点

```
$ python /opt/sequoiacm/sequoiacm-config/deploy.py --start
```

###增加调度服务节点###

- 编辑调度服务部署配置文件，配置需要新增的调度服务节点，如新增一个调度服务节点：

```
$ vi /opt/sequoiacm/sequoiacm-schedule/deploy.json
{
    "schedule-server":[
	    {
		    "hostname":"scmServer1",
			"server.port":"8180",
            "eureka.instance.metadata-map.zone": "zone1",
            "eureka.client.region": "beijing",
            "eureka.client.availability-zones.beijing": "zone1",
            "eureka.client.service-url.zone1": "http://scmServer2:8800/eureka/",
			"scm.zookeeper.urls":"zookeeperServer:2181",
			"scm.store.sequoiadb.urls":"sdbServer:11810",
			"scm.store.sequoiadb.username": "sdbadmin",
			"scm.store.sequoiadb.password": "/opt/sequoiacm/sdb.passwd",
            "scm.audit.userType.TOKEN":"ALL",
			"scm.jvm.options":"''"
		}
	],
    "audit":{
		"auditurl":"sdbServer:11810",
		"audituser":"sdbadmin",
		"auditpassword":"/opt/sequoiacm/sdb.passwd"
	}
}
```


>  **Note:**
>
>  * 注册中心地址、元数据储存地址需要按已有环境的配置填写
>
>  * 如果需要新增节点的机器上没有密码文件，即'/opt/sequoiacm/sdb.passwd'，可以从原有环境的部署主机上拷贝获取

- 部署并启动新增的调度服务节点

```
$ python /opt/sequoiacm/sequoiacm-schedule/deploy.py --start
```

