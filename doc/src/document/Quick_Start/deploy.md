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
|调度服务|scmServer:8180|提供调度管理功能|
|注册中心|scmServer:8800|提供服务治理功能|
|网关服务|scmServer:8080|提供路由、负载均衡功能|
|认证服务|scmServer:8810|提供认证、权限控制功能|
|监控服务|scmServer:8900|提供监控功能|

>  **Note：**
> 
>  * 本节假定用户已经具备版本匹配的 SequoiaDB ，不再介绍其部署步骤。后续介绍的安装部署命令均在 scmServer 机器上执行
>
>  * 请确保 SequoiaDB 为集群模式部署，并且已经开启事务功能，本章节假定 SequoiaDB 的用户名密码均为 sdbadmin

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
    >  * 本段配置元数据服务的连接信息，指定元数据存储的 Domain
       
	5). 数据服务配置段:

    ```
    [datasource]
    Name,  Type,      Url,                   User,     Password,    PasswordFile,  ConnectionConf,
    ds1,   sequoiadb, 'sdbServer:11810',     sdbadmin, sequoiadb,   
    ```

    >  **Note：**
    > 
    >  * 本段配置数据服务的连接信息，数据服务为站点提供存储支持
    
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
    
    9). 基础服务节点配置段:

    ```
    #ServiceType 可选值：service-center auth-server gateway config-server schedule-server
    [servicenode]
    ZoneName,  ServiceType,       HostName,       Port,  CustomNodeConf
    zone1,     service-center,    scmServer,      8800,  
    zone1,     auth-server,       scmServer,      8810,  
    zone1,     gateway,           scmServer,      8080,  
    zone1,     config-server,     scmServer,      8190,
    zone1,     schedule-server,   scmServer,      8910,
    zone1,     admin-server,      scmServer,      8900, 
    ```

    >  **Note：**
    > 
    >  * 本段配置用于描述各个微服务的节点配置
    

4. 执行部署

	```
	$ /opt/data/sequoiacm/scm.py cluster --deploy --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/deploy.cfg
	```
    
###配置业务###
1. 检查节点

    使用浏览器登陆服务注册中心（http://scmServer:8800），确保规划的节点都已经注册到服务注册中心。

2. 编辑工作区配置文件，创建一个名为test_ws的工作区

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
                ]
            }
        ]
    }
    ```

    > **Note:**
    >
    > * url 填写网关的地址，其中 rootsite 为主站点的服务名，在 url 中为全小写
    >
    > * userName 和 password 分别为系统默认的管理员用户密码

3. 创建工作区

	```
	$ /opt/data/sequoiacm/scm.py workspace --create --conf /opt/data/sequoiacm/sequoiacm-deploy/conf/workspaces.json
	```
    
4. 测试环境，将工作区权限赋给管理员用户，编辑权限配置，

    ```
    $ vi /opt/sequoiacm/sequoiacm-content/users.json
    {
        "url":"server1:8080/rootsite",
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
                ],
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

###安装部署完毕###

至此 SequoiaCM 系统已经安装完毕，可以使用 java 驱动连接 SequoiaCM 进行数据操作，操作示例详见[Java 开发基础][driver_operation]。

[install_requirement]:Quick_Start/install_requirement.md
[driver_operation]:Development/Java_Driver/Readme.md