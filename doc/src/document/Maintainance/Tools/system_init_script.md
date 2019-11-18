ContentServer 安装包下的 system.py 提供 SequoiaCM 环境初始化的功能。


##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--conf    |-c         |指定部署配置文件，默认为本脚本所在路径的 system.json 文件|
|--bin     |-b         |指定 SequoiaDB 的 bin 目录路径，默认值：/opt/sequoiadb/bin|
|--create  |           |创建 system.json 中配置的 domain、cs、cl|
|--clear   |           |清除 system.json 中配置的 domain、cs、cl|
|--drop-domain|         |清除 system.json 中配置的 domain|
|--drop-cs  |         |清除 system.json 中配置的 cs|
|--drop-cl  |         |清除 system.json 中配置的 cl|
|--dryrun  |           |仅打印脚本执行的命令，用于实际执行前的确认及核对|

##配置文件##
system.json 中配置了 SequoiaCM 需要的 domain、cs、cl，配置文件如下：

```lang-javascript
[
    {
        "sdbHost": "192.168.20.46",
        "sdbCoord": "11810",
        "sdbUserName": "sdbadmin",
        "sdbPassword": "sequoiadb",
        "domains":[
            {
                "name": "domain1",
                "groups": [
                    "group1"
                ],
                "options": {
                    "AutoSplit": true
                }
            },
            {
                "name": "domain2",
                "groups": [
                    "group1"
                ]
            }
        ],
        "collection_spaces":[
            {
                "name": "SCMSYSTEM",
                "options": {
                    "Domain": "domain1"
                }
            }
        ],
        "collections": [
            {
                "csName": "SCMSYSTEM",
                "clName": "SITE",
                "options":{
                
                },
                "indexes": [
                    {
                        "name": "idx_site_id",
                        "indexDef": {
                            "id": 1
                        },
                        "isUnique": "true",
                        "enforced": "false"
                    },
                    {
                        "name": "idx_site_name",
                        "indexDef": {
                            "name": 1
                        },
                        "isUnique": "true",
                        "enforced": "false"
                    }
                ]
            },
            .....省略部分 CL 配置
        ]
    },
    {
        "sdbHost": "192.168.20.46",
        "sdbCoord": "11810",
        "sdbUserName": "sdbadmin",
        "sdbPassword": "sequoiadb",
        "domains":[
            {
                "name": "domain1",
                "groups": [
                    "group1"
                ],
                "options": {
                    "AutoSplit": true
                }
            }
        ],
        "collection_spaces":[
            {
                "name": "SCMAUDIT",
                "options": {
                    "Domain": "domain1"
                }
            }
        ],
        "collections": [
            {
                "csName": "SCMAUDIT",
                "clName": "AUDIT_LOG_EVENT",
                "options":{
                    "IsMainCL": true,
                    "ShardingKey": {
                        "time": 1
                    },
                    "ShardingType": "range"
                }
            }
        ]
    }
]

````

>  **Note:**
> 
>  * system.json 需要用户修改其中的 SequoiaDB 地址信息，Domain 中包含的组信息。
> 
>  * 其中第一个 SequoiaDB 作为以后主站点元数据储存使用，第二个 SequoiaDB 作为审计日志存储使用。



