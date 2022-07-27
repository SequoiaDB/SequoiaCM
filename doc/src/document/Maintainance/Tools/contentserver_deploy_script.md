ContentServer 安装包下的 deploy.py 提供 ContentServer 服务的部署功能，deploy.py 本质上是通过 ContentServer 管理工具、ContentServer 节点控制工具实现的一个部署脚本，用户只需要配置相关的 json 文件，就可以通过该脚本搭建起 ContentServer 服务。搭建 ContentServer 服务前，需要先保证 Cloud 、Config 服务已搭建完成，并正常运行。

##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--clearcscl|          |是否清除 content-server 服务相关的系统表，默认值：False|
|--createsite|         |是否创建站点，默认值：False|
|--createnode|         |是否创建节点，默认值：False|
|--createws  |         |是否创建工作区，默认值：False|
|--createuser|         |是否创建用户及权限，默认值：False|
|--conf    |-c         |指定部署配置文件，默认为本脚本所在路径的 deploy.json 文件|
|--bin     |-b         |指定 ContentServer 工具目录，默认为本脚本所在路径的 bin 目录|
|--start   |-s         |部署完成后启动 ContentServer 节点|
|--dryrun  |           |仅打印脚本执行的命令，用于实际执行前的确认及核对|

##创建站点和节点##

搭建 ContentServer 服务，需要先建立站点和节点，通过 deploy.json 文件对站点和节点进行规划，如下是两站点，每个站点一个节点的配置示例：

```lang-javascript
{
    "sites": [
        {
            "name": "rootSite",
            "isRoot": true,
            "meta": {
                "url": "localhost:11810",
                "user": "sdbadmin",
                "password": "/opt/sequoiacm/secret/metasource.pwd"
            },
            "data": {
                "type": "sequoiadb",
                "url": "localhost:11810",
                "user": "sdbadmin",
                "password": "/opt/sequoiacm/secret/ds.pwd"
            }
        },
        {
            "name": "hbaseSite",
            "data": {
                "type": "hbase",
                "configuration": {
                    "hbase.zookeeper.quorum":"192.168.28.172:2181", 
                    "hbase.client.retries.number":"5", 
                    "hbase.client.pause":"50"
                }
            }
        }
    ],
    "nodes":[
        {
            "node": [
                {
                    "name": "rootNode",
                    "url": "localhost:15000",
                    "siteName": "rootSite",
                    "customProperties":{
                        "eureka.instance.metadata-map.zone": "zone1",
                        "eureka.client.region": "DefaultRegion",
                        "eureka.client.availability-zones.DefaultRegion": "zone1",
                        "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
                        "scm.zookeeper.urls": "localhost:2181"
                     }
                }
            ]
        },
        {
            "node": [
                {
                    "name": "hbaseSiteNode",
                    "url": "localhost:15100",
                    "siteName": "hbaseSite",
                    "customProperties":{
                        "eureka.instance.metadata-map.zone": "zone1",
                        "eureka.client.region": "DefaultRegion",
                        "eureka.client.availability-zones.DefaultRegion": "zone1",
                        "eureka.client.service-url.zone1": "http://localhost:8800/eureka/",
                        "scm.zookeeper.urls": "localhost:2181"
                     }
                }
            ]
        }
    ],
    "audit": {
            "auditurl":"localhost:11810",
            "audituser":"sdbadmin",
            "auditpassword":"/opt/sequoiacm/secret/auditsource.pwd"
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
>  * 整个配置文件是一个 JSON 文件，sites 字段描述站点配置，nodes 字段描述节点配置，audit 字段描述节点的审计日志配置。
> 
>  * site 字段的值表示一个站点的配置，其中的各个字段与 createsite 工具的参数一一对应
> 
>  * node 字段的值表示一个节点的配置，其中 customProperties 字段表示自定义的节点配置，支持的配置和配置的含义可以查看 [ContentServer 节点配置][contentserver_config]，其余字段与 createnode 工具参数对应。
> 
>  * audit 字段描述节点的审计日志配置，其中 auditurl 表示审计日志的入库地址（SequoiaDB），audituser 表示用户名，auditpassword 表示密码文件（密码文件通过 [encrypt 命令][encrypt_tool]生成）
> 
>  * deploy.py 目前只支持部署本机站点，所以 deploy.py 将会跳过 hostname 为非本机的节点配置。用户可以规划好配置文件后，拷贝到各个需要部署节点的机器上，分别执行 deploy.py --createnode。

执行以下命令创建配置的站点，创建并启动配置中的本机节点：

```lang-javascript
 $ deploy.py --createsite --createnode --start 
```

##创建工作区##
创建工作区需要保证相关站点已经建立，ContentServer 节点已经正常启动并注册到 ServiceCenter （可以通过访问 http://ServiceCenterHost:ServiceCenterPort 查看已注册的服务）。通过 workspace.json 对工作区进行配置，如下是一个配置文件示例：

```lang-javascript
{
    "url": "localhost:8080/rootsite",
    "userName": "admin",
    "password": "admin",    
    "workspaces": [
        {
            "name": "ws_default",
            "description": "'default sharding workspace'",
            "meta": {
                "site": "rootSite",
                "domain": "domain1",
                "meta_sharding_type": "year",
                "meta_options": {
                    "collection_space": {"LobPageSize":262144},
                    "collection": {"ReplSize":-1}
                }
            },
            "data": [
                {
                    "site": "rootSite",
                    "domain": "domain2",
                    "data_options": {
                        "collection_space": {"LobPageSize":262144},
                        "collection": {"ReplSize":-1}
                    }
                },
                {
                    "site": "hbaseSite"
                }
            ]
        }
    ]
}

```


>  **Note:**
> 
>  * 整个配置文件是一个 JSON 对象，url 字段描述一个已存在的站点地址（gatewayHost:gatewayPort/sitename），注意站点名小写，userName 字段描述管理员用户名，password 字段描述管理员密码，workspace 字段描述一组新建工作区的配置，其中各个字段与 [createws 命令][createws]参数对应。



执行以下命令创建配置的工作区：

```lang-javascript
 $ deploy.py --createws
```

##创建用户及授权##
创建用户及授权通过 user.json 进行配置，如下是创建一个具有 ws_default 所有权限的角色，并将该角色赋给一个新建用户和一个旧有用户的配置示例：

```lang-javascript
{
    "url": "localhost:8080/rootsite",
    "adminUser": "admin",
    "adminPassword": "admin",
    "roles": [
        {
            "name": "ROLE_ws_default",
            "resources": [
                {
                    "resourceType": "workspace",
                    "resource": "ws_default",
                    "privilege": "ALL"
                }
            ]
        }
    ],
    "newUsers": [
        {
            "name": "scmUser",
            "password": "scmPassword",
            "roles": ["ROLE_ws_default"]
        }
    ],
    "oldUsers": [
        {
            "name": "admin",
            "password": "admin",
            "roles": ["ROLE_ws_default"]
        }
    ]
}
```
>  **Note:**
> 
>  * 整个配置文件是一个 JSON 对象，url 字段描述一个已存在的站点地址（gatewayHost:gatewayPort/sitename），注意站点名小写，adminUser 字段描述管理员用户名，adminPassword 字段描述管理员密码，roles 字段描述一组新建角色，newUsers 字段描述一组新建用户，oldUsers 描述对一组旧有用户的角色配置。
> 
>  * roles 的值是一个 JSON 数组，每个元素描述一个新建角色的配置，其中 name 表示角色名，resources 表示该角色具备的一组资源的权限，resourceType 表示资源的类型，resource 表示资源名字，privilege 表示对资源具有何种权限。
> 
>  * newUsers 的值是一个 JSON 数组， 每个元素描述一个待新建的用户配置，其中 name 表示用户名，password 表示密码，roles 表示该用户具备的一组角色。
> 
>  * oldUsers 表示一组已存在的用户，脚本会将 rolse 字段中的角色赋予这些已存在的用户。


执行以下命令创建用户及授权：

```lang-javascript
 $ deploy.py --createuser
```

[contentserver_config]:Maintainance/Node_Config/contentserver.md
[encrypt_tool]:Maintainance/Tools/Scmadmin/encrypt.md
[createws]:Maintainance/Tools/Scmadmin/createws.md