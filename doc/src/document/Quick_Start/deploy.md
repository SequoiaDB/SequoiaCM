本节将介绍一个最小规模的 SequoiaCM 集群安装部署过程，所有服务仅有一个节点，内容管理服务仅部署一个站点。

###安装部署前准备###
请参照[软硬件配置要求][install_requirement],确保环境符合 SequoiaCM 的部署要求。

###部署规划###

|服务名|地址|说明|
|------|------------|----|
|SequoiaDB|sdbServer:11810|提供元数据存储服务和文件存储服务|
|ZooKeeper|scmServer:2181|zookeeper服务,提供选举及分布式锁服务|
|内容管理服务|scmServer:15000|提供内容管理核心功能|
|配置服务|scmServer:8190|提供配置管理功能|
|调度服务|scmServer:8910|提供调度管理功能|
|注册中心|scmServer:8800|提供服务治理功能|
|网关服务|scmServer:8080|提供路由、负载均衡功能|
|认证服务|scmServer:8810|提供认证、权限控制功能|
|监控服务|scmServer:8900|提供监控功能|
|消息队列服务|scmServer:8710|提供集群内消息队列功能|
|全文检索服务|scmServer:8610|提供文件的全文检索功能|
|OM管理服务|scmServer:9000|提供SequoiaCM集群的可视化管理功能|
|S3 服务|scmServer:16000|提供 S3 协议处理能力|

>  **Note：**
> 
>  * 本节假定用户已经部署了版本匹配的 SequoiaDB 和 Elasticsearch，同时为 Elasticsearch 安装了 IK 中文分词器
>  * 如果需要支持图片文件的全文检索，用户还需要自行在全文检索服务所在机器上安装 Tessract 图片识别引擎
>  * 请确保 SequoiaDB 为集群模式部署，并且已经开启事务功能，本章节假定 SequoiaDB 的用户名密码均为 sdbadmin
>  * 后续介绍的安装部署命令均在 scmServer 机器上执行


###安装包###

产品包名字以 sequoiacm-3.1.0-release.tar.gz 为例，压缩包内包含:

| 安装包         | 说明                                        |
|----------------|---------------------------------------------|
| driver      | 驱动包       |
| package   | 微服务集合包         |
| sequoiacm-deploy    | 部署工具|
| README.md | 说明       |
| scm.py | 部署脚本       |

###安装步骤###

1. 将 sequoiacm-3.1.0-release.tar.gz 压缩包上传至 scmServer

2. 解压缩

	```
	$ tar -zxvf sequoiacm-3.1.0-release.tar.gz -C /opt/data/
	```

3. 编辑集群部署规划文件

	```
	$ vi /opt/data/sequoiacm/sequoiacm-deploy/conf/deploy.cfg
	```

	>  **Note：**
	> 
	>  * deploy.cfg 是一个以 ini + csv 风格编写的配置文件，由多个配置段组成，每个配置段以 csv 进行表示，后续步骤介绍各个配置段的配置

	1). 安装配置段:

    ```
    # InstallUser 表示 sequoiacm 的安装用户，
    # 若 InstallUser 已存在，不需要填写 InstallUserPassword、InstallUserGroup
    # 若 InstallUser 不存在，必须填写 InstallUserPassword 用于建立 InstallUser，同时可以通过 InstallUserGroup 指定用户组
    [installconfig]
    InstallPath,          	InstallUser,  InstallUserPassword, InstallUserGroup
    /opt/,                  scmadmin,     admin,               scmadmin_group
    ```

    >  **Note：**
    > 
    >  * 本段配置表示 SequoiaCM 的安装路径在 /opt/ 下，使用 scmadmin 系统用户作为安装用户

	2). 机器配置段:

    ```
    #User 需要具备 sudo 权限
    #若机器已配置互信，并且 User 具备免密 sudo 的能力，可以不需要填写 Password
    #JavaHome 不填，默认在 /etc/profile 下搜索 JAVA_HOME 环境变量
    [host]
    HostName,      SshPort, User,        Password,  JavaHome   
    scmServer,     22,      sequoiadb,   sequoiadb, /opt/jdk1.8.0_11
    ```

    >  **Note：**
    > 
    >  * 本段配置部署涉及的机器，sequoiacm 将会在该段配置的机器上进行部署

	3). Zookeeper配置段:

    ```
    #zookeeper 节点部署数量建议为奇数
    [zookeeper]
    HostName,       ServerPort, ClientPort
    scmServer,      2888:3888,  2181
    ```

    >  **Note：**
    > 
    >  * 本段配置 zookeeper 集群的安装

	4). 元数据服务配置段:

    ```
    #只填 Password 列表示使用明文部署
    #填写 PasswordFile 列，Password 列表示密码文件的 token
    [metasource]
    Url,                   Domain ,  User,     Password,   PasswordFile
    'sdbServer:11810',     domain1,  sdbadmin, sequoiadb,
    ```

    >  **Note：**
    > 
    >  * 本段配置元数据服务的连接信息，指定元数据存储的 Domain ，该 Domain 需要用户预先手工创建
   
	5). 数据服务配置段:

    ```
    [datasource]
    Name,  Type,      Url,                   User,     Password,    PasswordFile,  ConnectionConf,
    ds1,   sequoiadb, 'sdbServer:11810',     sdbadmin, sequoiadb,   
    ```

    >  **Note：**
    > 
    >  * 本段配置数据服务的连接信息，数据服务为站点提供存储支持
    >  * 特别的，对于 Ceph S3 类型的数据源，允许在这里配备两个同名的 datasource，形成[主备数据源][primary_standby_cephs3]（顺序在前为主数据源）
   
	6). 站点配置段:

    ```
    [site]
    Name,        DatasourceName,    IsRootSite
    rootSite,    ds1,               true
    ```

    >  **Note：**
    > 
    >  * 本段配置一个站点，指定其数据服务，并作为主站点
   
    7). zone配置段:

    ```
    [zone]
    Name
    zone1
    ```

    >  **Note：**
    > 
    >  * 本段配置用于规划多机房集群，为了简易起见，此处设置一个机房，命名为zone1
   
    8). 内容服务节点配置段:

    ```
    [sitenode]
    ZoneName,  SiteName,    HostName,       Port,  CustomNodeConf
    zone1,     rootSite,    scmServer,      15000,
    ```

    >  **Note：**
    > 
    >  * 本段配置用于描述站点下的内容服务节点数量及配置

    9). S3 配置段:

    ```
    [s3node]
    ZoneName, BindingSite, HostName,        Port,   CustomNodeConf, ServiceName,  ManagementPort, 
    zone1,    rootSite,    scmServer,       16000 
    ```

    >  **Note：**
    > 
    >  * 本段配置 S3 服务节点的部署
   
    9). 基础服务节点配置段:

    ```
    [servicenode]
    ZoneName,  ServiceType,       HostName,       Port,  CustomNodeConf
    zone1,     service-center,    scmServer,      8800,  
    zone1,     auth-server,       scmServer,      8810,  
    zone1,     gateway,           scmServer,      8080,  
    zone1,     config-server,     scmServer,      8190,
    zone1,     schedule-server,   scmServer,      8910,
    zone1,     admin-server,      scmServer,      8900, 
    zone1,     mq-server,         scmServer,      8710, 
    zone1,     om-server,         scmServer,      9000, 
    zone1,     fulltext-server,   scmServer,      8610, '{"scm.fulltext.es.urls": "http://esServer:9200", "scm.fulltext.textualParser.pic.tessdataDir": "/usr/share/tesseract-ocr/tessdata/"}'
    ```

    >  **Note：**
    > 
    >  * 本段配置用于描述各个微服务的节点配置
    >  * 全文检索服务配置项 scm.fulltext.es.urls 表示 Elasticsearch 地址，scm.fulltext.textualParser.pic.tessdataDir 表示 Teesract 引擎训练数据所在目录
   
4. 执行部署

	```
	$ /opt/data/sequoiacm/scm.py cluster --deploy --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/deploy.cfg
	```

###配置业务###
1. 检查节点

    使用浏览器登陆服务注册中心（http://scmServer:8800），确保规划的节点都已经注册到服务注册中心。

2. 编辑工作区配置文件，创建一个名为 test_ws 的工作区

    ```
    $ vi /opt/data/sequoiacm/sequoiacm-deploy/conf/workspaces.json
    {
        "url":"scmServer:8080/rootsite",
        "userName": "admin",
        "password": "admin", 
        "workspaces":[
            {
                "name":"test_ws",
                "description":"''",
                "batch_sharding_type": "none",
                "batch_file_name_unique": false,
                "meta":{
                    "site":"rootSite",
                    "domain":"meta_domain",
                    "meta_sharding_type":"year"
                },
                "data":[
                    {
                        "site":"rootSite",
                        "domain":"data_domain",
                    }
                ],
                "enable_directory": false,
                "preferred": "rootSite"
            }
        ]
    }
    ```

    > **Note:**
    >
    > * url 填写网关的地址，其中 rootsite 为主站点的服务名，在 url 中为全小写
    > * userName 和 password 分别为系统默认的管理员用户密码
    > * enable_directory 为是否开启目录功能，默认为 true
    > * batch_sharding_type 为批次分区类型，默认为 none
    > * batch_file_name_unique 为批次内文件名是否唯一，默认 false
    > * meta 配置工作区元数据参数，其中 site 目前仅支持填写主站点，domain 填写元数据服务中的域，用于存储工作区元数据，该域需要用户预先手工创建
    > * data 配置可供工作区存储文件内容数据的站点列表，目前需要强制包含主站点，domain 填写站点数据服务中的域，该域需要用户预先手工创建
    > * 示例创建的工作区禁用了目录功能，如需启用请修改 enable_directory 为 true
    > * preferred 属性表示通过 S3 协议访问该工作区的资源时，网关将会选择指定站点上的 S3 服务进行转发
    
3. 创建工作区

	```
	$ /opt/data/sequoiacm/scm.py workspace --create --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/workspaces.json
	```

4. 测试环境，将工作区权限赋给管理员用户，编辑权限配置，

    ```
    $ vi /opt/sequoiacm/sequoiacm-content/users.json
    {
        "url":"scmServer:8080/rootsite",
        "adminUser":"admin",
        "adminPassword":"admin",
        "roles":[
            {
                "name":"test_role",
                "resources":[
                    {
                        "resourceType":"workspace",
                        "resource":"test_ws",
                        "privilege":"ALL"
                    }
                ]
            }
        ],
        "oldUsers": [
            {
                "name": "admin",
                "password": "admin",
                "roles": ["test_role"]
            }
        ],
        "newUsers": []
    }
    ```

5. 执行权限配置操作

    ```
    $ python /opt/sequoiacm/sequoiacm-content/createusers.py
    ```

6. 为 S3 服务设置默认区域

    ```
    $ /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh  set-default-region --region test_ws --user admin --password admin --url scmServer:8080
    ```

    > **Note:**
    >
    > * S3 服务将 SequoiaCM 的工作区映射为 S3 Region，设置默认 Region 即向 S3 指定一个工作区（该工作区需要禁用目录）
    > * 默认 Region 的作用是：后续在 S3 创建 Bucket 不指定 Region 时，将会落在该默认 Region 上

7. 生成 S3 Access Key 和 Secret Key

    ```
    $ /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh  refresh-accesskey --target-user admin --user admin --password admin --s3-url scmServer:16000
    ```

    > **Note:**
    >
    > * 该命令将会在控制台输出 Access Key 和 Secret Key，后续客户端访问 SequoiaCM S3 接口需要通过这对 Key 进行鉴权

###安装部署完毕###

至此 SequoiaCM 系统已经安装完毕，可以使用 SequoiaCM java 驱动或 S3 客户端连接 SequoiaCM 进行数据操作，操作示例详见[Java 开发基础][driver_operation]。

###卸载 SequoiaCM ###

1. 需卸载 SequoiaCM 的情况

   - 已不再需要使用 SequoiaCM
   - 需要重新部署 SequoiaCM 集群

2. 清理工作区

   ```
   $ /opt/data/sequoiacm/scm.py workspace --clean --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/workspaces.json
   ```

3. 卸载 SequoiaCM 集群

   ```
   $ /opt/data/sequoiacm/scm.py cluster --clean --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/deploy.cfg
   ```

   > **Note：**
   >
   > * 卸载 SequoiaCM 时，一定要先清理工作区，否则会导致 SequoiaDB 中残留存放工作区信息的集合空间，将会影响集群的下一次部署
   > * 卸载成功之后，SequoiaDB 中存放工作区元数据和内容数据的集合空间、所有服务节点目录都将会被删除，并且所有服务节点都将被关闭

[install_requirement]:Quick_Start/install_requirement.md
[driver_operation]:Development/Java_Driver/Readme.md
[primary_standby_cephs3]:Architecture/data_storage.md